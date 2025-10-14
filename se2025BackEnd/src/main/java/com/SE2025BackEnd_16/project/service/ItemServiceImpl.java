package com.SE2025BackEnd_16.project.service;

import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dao.ItemDao;
import com.SE2025BackEnd_16.project.dto.converter.ItemConverter;
import com.SE2025BackEnd_16.project.dto.request.ItemCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemUpdateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemDeleteRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemToggleAvailabilityRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemQueryRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ItemResponseDTO;
import com.SE2025BackEnd_16.project.dto.response.PageResponseDTO;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.Category;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.entity.Favorite;
import com.SE2025BackEnd_16.project.entity.UserViewRecord;
import com.SE2025BackEnd_16.project.repository.CategoryRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.repository.FavoriteRepository;
import com.SE2025BackEnd_16.project.repository.UserViewRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 商品服务实现类
 */
@Slf4j
@Service
@Transactional
public class ItemServiceImpl implements ItemService {
    
    @Autowired
    private ItemDao itemDao;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private UserViewRecordRepository userViewRecordRepository;
    
    @Autowired
    private ItemConverter itemConverter;
    
    @Autowired
    private OSSService ossService;
    
    @Value("${app.test.mode:false}")
    private boolean testMode;
    
    @Value("${app.test.default-image-url:/default-product-image.png}")
    private String defaultImageUrl;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private KafkaUtils kafkaUtils;

    private static final AtomicInteger tempIdGenerator = new AtomicInteger(-1);

    @Override
    public ItemResponseDTO createItem(ItemCreateRequestDTO requestDTO) {
        log.info("开始发布商品: {}", requestDTO.getTitle());
        
        // 1. 验证卖家是否存在
        if (!userInfoRepository.existsById(requestDTO.getSellerId())) {
            throw new RuntimeException("卖家用户不存在");
        }
        
        // 2. 根据分类名称查找分类ID
        Optional<Category> categoryOpt = categoryRepository.findByCategoryName(requestDTO.getCategory());
        if (categoryOpt.isEmpty()) {
            throw new RuntimeException("商品分类不存在: " + requestDTO.getCategory());
        }
        Category category = categoryOpt.get();
        
        // 3. 处理图片上传
        String imageUrl;
        if (testMode) {
            // 测试模式下，直接使用默认图片URL
            imageUrl = defaultImageUrl;
            log.info("测试模式: 使用默认图片URL: {}", imageUrl);
        } else {
            // 生产模式下，上传到OSS
            try {
                imageUrl = ossService.uploadFile(requestDTO.getImage(), "items");
                log.info("图片上传成功: {}", imageUrl);
            } catch (Exception e) {
                log.error("图片上传失败: {}", e.getMessage());
                throw new RuntimeException("图片上传失败: " + e.getMessage());
            }
        }
        
        // 4. DTO转Entity
        Item item = itemConverter.toEntity(requestDTO, category.getCategoryId(), imageUrl);
        
        // 5. 生成临时负数ID
        int tempItemId = tempIdGenerator.getAndDecrement();
        item.setItemId(tempItemId);
        item.setIsAvailable(true);
        item.setUpdateTime(java.time.LocalDateTime.now()); // 保证有时间分数
        
        // 6. 写入缓存和集合
        redisUtils.cacheItemDetail(tempItemId, item); // 新增重载方法
        redisUtils.addItemToCategorySet(category.getCategoryId(), tempItemId);
        redisUtils.addItemToSellerSet(requestDTO.getSellerId(), tempItemId);
        // 新增：临时ID阶段也加入首页可售商品Sorted Set
        redisUtils.addItemToOnSaleSortedSets(tempItemId, item);
        // 新增：临时ID阶段也加入分类可售商品Sorted Set
        double updateScore = item.getUpdateTime() != null
            ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
            : System.currentTimeMillis() / 1000.0;
        double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
        double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
        redisUtils.addItemToCategorySortedSet(category.getCategoryId(), tempItemId, updateScore, "update_time");
        redisUtils.addItemToCategorySortedSet(category.getCategoryId(), tempItemId, priceScore, "price");
        redisUtils.addItemToCategorySortedSet(category.getCategoryId(), tempItemId, likesScore, "likes");
        
        // 7. 发送Kafka消息，异步入库
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            kafkaUtils.sendMessage("createItem", mapper.writeValueAsString(item));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("商品发布消息序列化失败: " + e.getMessage(), e);
        }
        
        // 8. 返回临时ID和基本信息
        UserInfo seller = userInfoRepository.findById(requestDTO.getSellerId()).orElse(null);
        String sellerName = seller != null ? seller.getUsername() : "";
        return itemConverter.toResponseDTO(item, category.getCategoryName(), sellerName, List.of(imageUrl));
    }
    
    @Override
    @Transactional(readOnly = true)
    public ItemResponseDTO getItemById(Integer itemId) {
//        log.info("获取商品详情，商品ID: {}", itemId);
        
        Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("商品不存在，ID: " + itemId);
        }
        
        Item item = itemOpt.get();
        
        // 记录浏览记录 - 使用固定用户ID（实际应用中应从认证信息获取）
        recordUserViewAsync(1, item.getCategoryId());
        
        return buildItemResponseDTO(item);
    }
    
    /**
     * 异步记录用户浏览记录
     */
    private void recordUserViewAsync(Integer userId, Integer categoryId) {
        try {
            // 查找是否已有该用户和分类的记录
            Optional<UserViewRecord> existingRecord = userViewRecordRepository
                    .findByUserIdAndCategoryId(userId, categoryId);
            
            if (existingRecord.isPresent()) {
                // 更新浏览次数
                UserViewRecord record = existingRecord.get();
                record.setCategoryViewCounts(record.getCategoryViewCounts() + 1);
                record.setViewTime(Timestamp.valueOf(LocalDateTime.now()));
                userViewRecordRepository.save(record);
            } else {
                // 创建新记录
                UserViewRecord newRecord = new UserViewRecord();
                newRecord.setUserId(userId);
                newRecord.setCategoryId(categoryId);
                newRecord.setCategoryViewCounts(1);
                newRecord.setViewTime(Timestamp.valueOf(LocalDateTime.now()));
                userViewRecordRepository.save(newRecord);
            }
            
            log.info("浏览记录已更新: 用户{}, 分类{}", userId, categoryId);
        } catch (Exception e) {
            log.error("记录浏览记录失败: 用户{}, 分类{}, 错误: {}", userId, categoryId, e.getMessage());
            // 浏览记录失败不影响主要功能，只记录错误日志
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getAllItems() {
        List<Item> items = itemDao.findAll();
        return items.stream()
                .map(this::buildItemResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getItemsBySellerId(Integer sellerId) {
        // 先查Redis
        List<Integer> itemIds = redisUtils.getSellerItemIds(sellerId, 0, 100); // 可加分页参数
        List<Item> items = redisUtils.getAllItemsByIds(itemIds); // 返回所有商品，包括下架
        if (items.isEmpty()) {
            // Redis未命中查库并补缓存和集合
            System.out.println("Redis未命中查库并补缓存和集合");
            items = itemDao.findBySellerId(sellerId);
            for (Item item : items) {
                redisUtils.cacheItemDetail(item.getItemId());
                // 不再直接补集合，改为异步
            }
            // 发送Kafka消息异步预热卖家商品集合缓存
            kafkaUtils.sendMessage("sellerItemsCache", String.valueOf(sellerId));
        }
        else{
            System.out.println("Redis命中" + items.size() + "个商品");
        }
        return items.stream().map(this::buildItemResponseDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getItemsByCategoryId(Integer categoryId) {
//        List<Item> items = itemDao.findByCategoryId(categoryId);
        List<Item> items = redisUtils.getCategoryItems(categoryId);
        if (items == null) items = List.of();
        if (items.isEmpty()) {

            items = itemDao.findByCategoryId(categoryId);
            if(!items.isEmpty()) {
                System.out.println("商品种类" + categoryId + "缓存未命中");
                kafkaUtils.sendMessage("itemCategory" , String.valueOf(categoryId));
            }
        }
        else{
            System.out.println("缓存命中" + categoryId + ": " + items.size());
        }
        return items.stream()
                .map(this::buildItemResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getAvailableItems() {
        List<Item> items = itemDao.findByIsAvailable(true);
        return items.stream()
                .map(this::buildItemResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ItemResponseDTO updateItem(Integer itemId, ItemUpdateRequestDTO requestDTO) {
        // 1. 先查缓存
        Item item = redisUtils.getItemDetail(itemId);
        if (item == null) {
            System.out.println("缓存未命中" + itemId);
            Optional<Item> itemOpt = itemDao.findById(itemId);
            if (itemOpt.isEmpty()) throw new RuntimeException("商品不存在，ID: " + itemId);
            item = itemOpt.get();
        }
        // 2. 权限校验
        if (!item.getSellerId().equals(requestDTO.getSellerId())) {
            throw new RuntimeException("无权限修改此商品");
        }
        // 3. 处理图片上传
        String newImageUrl = null;
        if (requestDTO.hasNewImage()) {
            if (testMode) {
                // 测试模式下，直接使用默认图片URL
                newImageUrl = defaultImageUrl;
                log.info("测试模式: 更新商品使用默认图片URL: {}", newImageUrl);
            } else {
                // 生产模式下，上传到OSS
                newImageUrl = ossService.uploadFile(requestDTO.getImage(), "items");
            }
            item.setImageUrl(newImageUrl);
        }
        // 4. 更新缓存中的商品字段
        item.setItemName(requestDTO.getTitle());
        item.setPrice(requestDTO.getPrice());
        item.setDescription(requestDTO.getDescription());
        item.setItemCondition(requestDTO.getCondition());
        // 分类变更
        Integer oldCategoryId = item.getCategoryId();
        Integer newCategoryId = categoryRepository.findByCategoryName(requestDTO.getCategory())
            .orElseThrow(() -> new RuntimeException("商品分类不存在: " + requestDTO.getCategory()))
            .getCategoryId();
        item.setCategoryId(newCategoryId);
        // 5. set回缓存
        redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 60, java.util.concurrent.TimeUnit.MINUTES);
        // 6. 发送Kafka消息，内容包含itemId, oldCategoryId, newCategoryId, newImageUrl
        String kafkaMsg = itemId + "," + oldCategoryId + "," + newCategoryId + "," + (newImageUrl != null ? newImageUrl : "");
        kafkaUtils.sendMessage("updateItem", kafkaMsg);
        // 7. 返回最新DTO
        return buildItemResponseDTO(item);
    }
    

    
    @Override
    public void toggleItemAvailability(Integer itemId, ItemToggleAvailabilityRequestDTO requestDTO) {
        // 1. 查缓存
        Item item = redisUtils.getItemDetail(itemId);
        if (item == null) {
            System.out.println("缓存未命中，查询数据库");
            Optional<Item> itemOpt = itemDao.findById(itemId);
            if (itemOpt.isEmpty()) throw new RuntimeException("商品不存在，ID: " + itemId);
            item = itemOpt.get();
        }
        else{
            System.out.println("缓存命中，查询缓存");
        }
        // 2. 权限校验
        if (!item.getSellerId().equals(requestDTO.getOperatorId())) {
            throw new RuntimeException("无权限操作此商品");
        }
        // 3. 更新缓存
        item.setIsAvailable(requestDTO.getIsAvailable());
        redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 60, java.util.concurrent.TimeUnit.MINUTES);
        // 4. 分类集合和Sorted Set同步
        int categoryId = item.getCategoryId();
        if (requestDTO.getIsAvailable()) {
            // 上架：加入分类集合和所有Sorted Set
            item.setUpdateTime(java.time.LocalDateTime.now());
            redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 60, java.util.concurrent.TimeUnit.MINUTES);

    // 先移除再添加，防止脏数据
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "update_time");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "price");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "likes");

            redisUtils.addItemToCategorySet(categoryId, itemId);
            double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
            double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
            double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
            redisUtils.addItemToCategorySortedSet(categoryId, itemId, updateScore, "update_time");
            redisUtils.addItemToCategorySortedSet(categoryId, itemId, priceScore, "price");
            redisUtils.addItemToCategorySortedSet(categoryId, itemId, likesScore, "likes");
            // 首页可售商品Sorted Set
            redisUtils.addItemToOnSaleSortedSet(itemId, updateScore, "update_time");
            redisUtils.addItemToOnSaleSortedSet(itemId, priceScore, "price");
            redisUtils.addItemToOnSaleSortedSet(itemId, likesScore, "likes");
        } else {
            // 下架：移除分类集合和所有Sorted Set
            redisUtils.removeItemFromCategorySet(categoryId, itemId);
            redisUtils.removeItemFromCategorySortedSet(categoryId, itemId, "update_time");
            redisUtils.removeItemFromCategorySortedSet(categoryId, itemId, "price");
            redisUtils.removeItemFromCategorySortedSet(categoryId, itemId, "likes");
            // 首页可售商品Sorted Set
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "update_time");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "price");
            redisUtils.removeItemFromOnSaleSortedSet(itemId, "likes");
        }
        // 5. 从可售集合移除或加入
        redisUtils.updateItemAvailability(itemId, requestDTO.getIsAvailable());
        // 6. 发送Kafka消息，异步更新数据库
        String kafkaMsg = itemId + "," + requestDTO.getIsAvailable() + "," + requestDTO.getOperatorId();
        kafkaUtils.sendMessage("toggleItemAvailability", kafkaMsg);
        log.info("商品{}成功，ID: {}, 操作者: {}", 
                requestDTO.getOperationDescription(), itemId, requestDTO.getOperatorId());
    }
    
    @Override
    public void likeItem(Integer itemId) {
        itemDao.incrementLikes(itemId);
        log.info("商品点赞成功，ID: {}", itemId);
    }
    
    @Override
    public void unlikeItem(Integer itemId) {
        itemDao.decrementLikes(itemId);
        log.info("商品取消点赞成功，ID: {}", itemId);
    }
    
    @Override
    public void deleteItem(Integer itemId, ItemDeleteRequestDTO requestDTO) {
        // 验证商品是否存在
        Optional<Item> itemOpt = itemDao.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("商品不存在，ID: " + itemId);
        }
        
        Item item = itemOpt.get();
        
        // 验证删除权限
        if (!requestDTO.validateDeletePermission(item.getSellerId())) {
            throw new RuntimeException("无权限删除此商品");
        }
        
        // 根据forceDelete参数决定删除方式
        if (requestDTO.getForceDelete() != null && requestDTO.getForceDelete()) {
            // 硬删除：直接从数据库删除
            itemDao.deleteById(itemId);
            log.info("商品硬删除成功，ID: {}, 操作者: {}", 
                    itemId, requestDTO.getOperatorId());
        } else {
            // 软删除：标记为已删除
            itemDao.softDeleteItem(itemId);
            log.info("商品软删除成功，ID: {}, 操作者: {}", 
                    itemId, requestDTO.getOperatorId());
        }
    }
    
    @Override
    public void restoreItem(Integer itemId, Integer operatorId) {
        // 使用包括已删除商品的查询方法
        Optional<Item> itemOpt = itemDao.findByIdIncludingDeleted(itemId);
        if (itemOpt.isEmpty()) {
            throw new RuntimeException("商品不存在，ID: " + itemId);
        }
        
        Item item = itemOpt.get();
        
        // 验证权限（只有商品所有者可以恢复）
        if (!item.getSellerId().equals(operatorId)) {
            throw new RuntimeException("无权限恢复此商品");
        }
        
        // 检查商品是否已被删除
        if (!item.getIsDeleted()) {
            throw new RuntimeException("商品未被删除，无需恢复");
        }
        
        // 恢复商品
        itemDao.restoreItem(itemId);
        log.info("商品恢复成功，ID: {}, 操作者: {}", itemId, operatorId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getAllItemsBySellerIdIncludingDeleted(Integer sellerId) {
        List<Item> items = itemDao.findAllBySellerIdIncludingDeleted(sellerId);
        return items.stream()
                .map(this::buildItemResponseDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public PageResponseDTO<ItemResponseDTO> queryItems(ItemQueryRequestDTO requestDTO) {
        log.info("分页查询商品: {}", requestDTO);
        try {
            // 只对未指定分类的全局分页生效
            boolean useGlobalRedis = requestDTO.getCategoryId() == null;
            int page = requestDTO.getPage();
            int size = requestDTO.getSize();
            String sortBy = requestDTO.getSortBy() != null ? requestDTO.getSortBy() : "update_time";
            if(sortBy.equals("updateTime")) {
                sortBy = "update_time";
            }
            boolean desc = requestDTO.isDescending();
            if (useGlobalRedis) {
                // 只查可售商品集合
                List<Integer> itemIds = redisUtils.getOnSaleItemIdsSorted(page, size, desc, sortBy);

                // 输出到本地文件调试
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append("page=").append(page).append(",size=").append(size).append(",sortBy=").append(sortBy).append(",desc=").append(desc).append("\n");
                    for (Integer id : itemIds) {
                        sb.append(id).append(",");
                    }
                    sb.append("\n");
                    Files.write(Paths.get("item_ids_debug.txt"), sb.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (Exception e) {
                    System.err.println("写入item_ids_debug.txt失败: " + e.getMessage());
                }
                // 按顺序获取商品详情
                List<Item> items = itemIds.stream()
                    .map(redisUtils::getItemDetail)
                    .filter(item -> item != null && Boolean.TRUE.equals(item.getIsAvailable()))
                    .collect(Collectors.toList());
                long total = redisUtils.getOnSaleItemCount(sortBy);
                List<ItemResponseDTO> responseItems = items.stream().map(this::buildItemResponseDTO).collect(Collectors.toList());
                System.out.println("items: " + responseItems.size());
                return PageResponseDTO.of(responseItems, page, size, total);
            } else {
                // 分类分页如需支持Redis分页可单独实现，否则抛异常或返回空
                throw new UnsupportedOperationException("分类分页请使用专用接口");
            }
        } catch (Exception e) {
            log.error("分页查询商品失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询商品失败: " + e.getMessage());
        }
    }
    
    public PageResponseDTO<ItemResponseDTO> getItemsByCategoryPaged(Integer categoryId, int page, int size, String sortBy, String sortDirection) {
        boolean desc = "DESC".equalsIgnoreCase(sortDirection);
        if (sortBy == null || sortBy.isEmpty()) sortBy = "update_time";

        if(sortBy.equals("updateTime")){
            sortBy = "update_time";
        }
        String finalSortBy = sortBy;

        final boolean finalDesc = desc;
        // 1. 优先从Redis获取分页ID
        List<Integer> itemIds = redisUtils.getCategoryItemIdsSorted(categoryId, page, size, desc, sortBy);
        List<Item> items = new ArrayList<>();
        for (Integer id : itemIds) {
            Item item = redisUtils.getItemDetail(id);
            if (item != null && Boolean.TRUE.equals(item.getIsAvailable())) items.add(item); // 只返回可售商品
        }
        // 2. 如果Redis未命中，查库并补充缓存
        if (items.isEmpty()) {
            System.out.println("缓存未命中category： " + categoryId );
            items = itemDao.findByCategoryId(categoryId);
            // 按sortBy排序
            items = items.stream().filter(i -> Boolean.TRUE.equals(i.getIsAvailable())).collect(Collectors.toList()); // 只要可售
            items.sort((a, b) -> {
                int cmp = 0;
                if ("update_time".equals(finalSortBy)) {
                    cmp = a.getUpdateTime().compareTo(b.getUpdateTime());
                } else if ("likes".equals(finalSortBy)) {
                    cmp = Integer.compare(a.getLikes(), b.getLikes());
                } else if ("price".equals(finalSortBy)) {
                    cmp = a.getPrice().compareTo(b.getPrice());
                }
                return finalDesc ? -cmp : cmp;
            });
            // 分页
            int totalElements = items.size();
            int from = Math.min(page * size, totalElements);
            int to = Math.min(from + size, totalElements);
            List<Item> pageItems = items.subList(from, to);
            // 补充缓存
            for (Item item : items) {
                redisUtils.cacheItemDetail(item.getItemId());
                double updateScore = item.getUpdateTime() != null ? item.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() : System.currentTimeMillis() / 1000.0;
                double priceScore = item.getPrice() != null ? item.getPrice().doubleValue() : 0.0;
                double likesScore = item.getLikes() != null ? item.getLikes() : 0.0;
                redisUtils.addItemToCategorySortedSet(categoryId, item.getItemId(), updateScore, "update_time");
                redisUtils.addItemToCategorySortedSet(categoryId, item.getItemId(), priceScore, "price");
                redisUtils.addItemToCategorySortedSet(categoryId, item.getItemId(), likesScore, "likes");
            }
            List<ItemResponseDTO> dtos = pageItems.stream().map(this::buildItemResponseDTO).collect(Collectors.toList());
            return PageResponseDTO.of(dtos, page, size, (long)totalElements);
        }
        // Redis命中时也要构造分页元数据
        long totalElements = redisUtils.getCategoryItemCount(categoryId, sortBy);
        List<ItemResponseDTO> dtos = items.stream().map(this::buildItemResponseDTO).collect(Collectors.toList());
        return PageResponseDTO.of(dtos, page, size, totalElements);
    }
    
    // ==================== 收藏相关方法实现 ====================
    


    ItemResponseDTO buildItemResponseDTO(Item item) {
        // 获取分类信息
        Category category = categoryRepository.findById(item.getCategoryId()).orElse(null);
        String categoryName = category != null ? category.getCategoryName() : "";
        
        // 获取卖家信息
        UserInfo seller = userInfoRepository.findById(item.getSellerId()).orElse(null);
        String sellerName = seller != null ? seller.getUsername() : "";
        
        // 构建图片URL列表（目前只有主图片）
        List<String> imageUrls = item.getImageUrl() != null ? 
                List.of(item.getImageUrl()) : List.of();
        
        return itemConverter.toResponseDTO(item, categoryName, sellerName, imageUrls);
    }
    
    // ==================== 收藏相关方法实现 ====================
    
    @Override
    @Transactional(readOnly = true)
    public boolean checkItemFavoriteStatus(Integer userId, Integer itemId) {
        log.info("检查用户{}对商品{}的收藏状态", userId, itemId);
        return favoriteRepository.existsByUserIdAndItemId(userId, itemId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getItemFavoriteCount(Integer itemId) {
        log.info("获取商品{}的收藏数量", itemId);
        return favoriteRepository.countByItemId(itemId);
    }
    
    @Override
    public void addItemFavorite(Integer userId, Integer itemId) {
        log.info("用户{}收藏商品{}", userId, itemId);
        // 检查是否已收藏
        if (!favoriteRepository.existsByUserIdAndItemId(userId, itemId)) {
            try {
                Favorite favorite = new Favorite();
                favorite.setUserId(userId);
                favorite.setItemId(itemId);
                favoriteRepository.save(favorite);
                // 立即更新商品缓存的likes字段+1（只更新itemDetail缓存）
                Item item = redisUtils.getItemDetail(itemId);
                if (item != null) {
                    int newLikes = (item.getLikes() == null ? 0 : item.getLikes()) + 1;
                    item.setLikes(newLikes);
                    redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 60, java.util.concurrent.TimeUnit.MINUTES);
                }
                // 发送kafka消息异步修改数据库和ZSet
                kafkaUtils.sendMessage("favoriteItem", String.valueOf(itemId));
                log.info("收藏成功: 用户{}, 商品{}", userId, itemId);
            } catch (Exception e) {
                log.error("收藏失败: 用户{}, 商品{}, 错误: {}", userId, itemId, e.getMessage());
                throw new RuntimeException("收藏失败: " + e.getMessage());
            }
        } else {
            log.info("用户{}已收藏商品{}", userId, itemId);
            throw new RuntimeException("已收藏该商品");
        }
    }
    
    @Override
    public void removeItemFavorite(Integer userId, Integer itemId) {
        log.info("用户{}取消收藏商品{}", userId, itemId);
        try {
            if (favoriteRepository.existsByUserIdAndItemId(userId, itemId)) {
                favoriteRepository.deleteByUserIdAndItemId(userId, itemId);
                // 立即更新商品缓存的likes字段-1（只更新itemDetail缓存）
                Item item = redisUtils.getItemDetail(itemId);
                if (item != null) {
                    int newLikes = (item.getLikes() == null ? 0 : item.getLikes()) - 1;
                    if (newLikes < 0) newLikes = 0;
                    item.setLikes(newLikes);
                    redisUtils.setCache(redisUtils.generateItemDetailKey(itemId), item, 60, java.util.concurrent.TimeUnit.MINUTES);
                }
                // 发送kafka消息异步修改数据库和ZSet
                kafkaUtils.sendMessage("unfavoriteItem", String.valueOf(itemId));
                log.info("取消收藏成功: 用户{}, 商品{}", userId, itemId);
            } else {
                log.info("用户{}未收藏商品{}", userId, itemId);
                throw new RuntimeException("未收藏该商品");
            }
        } catch (Exception e) {
            log.error("取消收藏失败: 用户{}, 商品{}, 错误: {}", userId, itemId, e.getMessage());
            throw new RuntimeException("取消收藏失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDTO> getUserFavoriteItems(Integer userId) {
        log.info("获取用户{}的收藏商品列表", userId);
        
        try {
            List<Favorite> favorites = favoriteRepository.findByUserId(userId);
            List<Integer> itemIds = favorites.stream()
                    .map(Favorite::getItemId)
                    .collect(Collectors.toList());
            
            List<Item> items = itemDao.findAllById(itemIds);
            return items.stream()
                    .map(this::buildItemResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户收藏商品失败: 用户{}, 错误: {}", userId, e.getMessage());
            throw new RuntimeException("获取收藏商品失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserInfo> getItemFavoriteUsers(Integer itemId) {
        log.info("获取收藏商品{}的用户列表", itemId);
        
        try {
            List<Favorite> favorites = favoriteRepository.findByItemId(itemId);
            List<Integer> userIds = favorites.stream()
                    .map(Favorite::getUserId)
                    .collect(Collectors.toList());
            
            return userInfoRepository.findAllById(userIds);
        } catch (Exception e) {
            log.error("获取商品收藏用户失败: 商品{}, 错误: {}", itemId, e.getMessage());
            throw new RuntimeException("获取收藏用户失败: " + e.getMessage());
        }
    }
    
    // ==================== 浏览记录相关方法实现 ====================
    
    @Override
    @Transactional(readOnly = true)
    public List<Object> getUserViewRecords(Integer userId) {
        log.info("获取用户浏览记录统计，用户ID: {}", userId);
        
        try {
            List<UserViewRecord> records = userViewRecordRepository.findByUserId(userId);
            return records.stream()
                    .map(record -> {
                        // 获取分类名称
                        String categoryName = categoryRepository.findById(record.getCategoryId())
                                .map(Category::getCategoryName)
                                .orElse("未知分类");
                        
                        // 返回包含分类名称的统计信息
                        return Map.of(
                                "categoryId", record.getCategoryId(),
                                "categoryName", categoryName,
                                "viewCount", record.getCategoryViewCounts(),
                                "lastViewTime", record.getViewTime()
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户浏览记录失败: 用户{}, 错误: {}", userId, e.getMessage());
            throw new RuntimeException("获取浏览记录失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Object> getPopularCategories() {
        log.info("获取热门分类统计");
        
        try {
            List<Category> categories = categoryRepository.findAll();
            return categories.stream()
                    .map(category -> {
                        // 获取分类总浏览次数
                        Long totalViews = userViewRecordRepository.getTotalViewsByCategoryId(category.getCategoryId());
                        
                        return Map.of(
                                "categoryId", category.getCategoryId(),
                                "categoryName", category.getCategoryName(),
                                "totalViews", totalViews != null ? totalViews : 0L,
                                "icon", category.getIcon() != null ? category.getIcon() : ""
                        );
                    })
                    .sorted((a, b) -> Long.compare(
                            (Long) b.get("totalViews"), 
                            (Long) a.get("totalViews")
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取热门分类失败: {}", e.getMessage());
            throw new RuntimeException("获取热门分类失败: " + e.getMessage());
        }
    }
} 