package com.SE2025BackEnd_16.project.Controller;

import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dto.converter.ItemConverter;
import com.SE2025BackEnd_16.project.dto.request.ItemCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemUpdateRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemDeleteRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemToggleAvailabilityRequestDTO;
import com.SE2025BackEnd_16.project.dto.request.ItemQueryRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ApiResponse;
import com.SE2025BackEnd_16.project.dto.response.ItemResponseDTO;
import com.SE2025BackEnd_16.project.dto.response.PageResponseDTO;

import com.SE2025BackEnd_16.project.entity.Category;
import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.CategoryRepository;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;

import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.entity.ItemDocument;
import com.SE2025BackEnd_16.project.service.ItemService;
import com.SE2025BackEnd_16.project.service.ProductDescriptionService;
import com.SE2025BackEnd_16.project.service.ItemSearchService;
import com.SE2025BackEnd_16.project.service.ElasticsearchSyncService;
import com.SE2025BackEnd_16.project.utils.JwtAuthHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;


import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;

/**
 * 商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/items")
@Validated
@CrossOrigin(origins = "*") // 允许跨域
public class ItemController {
    
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private JwtAuthHelper jwtAuthHelper;



    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private KafkaUtils kafkaUtils;

    @Autowired(required = false)
    private ProductDescriptionService productDescriptionService;
    
    @Autowired(required = false)
    private ItemSearchService itemSearchService;
    
    @Autowired(required = false)
    private ElasticsearchSyncService elasticsearchSyncService;


    
    /**
     * 发布商品
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponseDTO>> createItem(
            @RequestParam("title") String title,
            @RequestParam("price") String price,
            @RequestParam("category") String category,
            @RequestParam("condition") String condition,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("image") MultipartFile image,
            @RequestParam("sellerId") Integer sellerId,
            HttpServletRequest request) {
        
        try {

//            log.info("接收到发布商品请求: title={}, price={}, category={}, condition={}", title, price, category, condition);
//            log.info("接收到的所有参数: title={}, price={}, category={}, condition={}, description={}, sellerId={}",
//                    title, price, category, condition, description, sellerId);
//
//            // 打印所有接收到的参数
//            log.info("=== 调试信息 ===");
//            log.info("condition 参数值: [{}]", condition);
//            log.info("condition 参数长度: {}", condition != null ? condition.length() : "null");
//            log.info("condition 是否为空: {}", condition == null || condition.trim().isEmpty());
//
//            // 打印所有请求参数
//            log.info("所有请求参数:");
            request.getParameterMap().forEach((key, values) -> {
//                log.info("  {} = {}", key, java.util.Arrays.toString(values));

            });
            
            // 构建RequestDTO
            ItemCreateRequestDTO requestDTO = new ItemCreateRequestDTO();
            requestDTO.setTitle(title);
            requestDTO.setPrice(price);
            requestDTO.setCategory(category);
            requestDTO.setCondition(condition);
            requestDTO.setDescription(description);
            requestDTO.setImage(image);
            requestDTO.setSellerId(sellerId);
            

//            log.info("构建的RequestDTO: title={}, price={}, category={}, condition={}",
//                    requestDTO.getTitle(), requestDTO.getPrice(), requestDTO.getCategory(), requestDTO.getCondition());

            
            // 调用Service层
            ItemResponseDTO responseDTO = itemService.createItem(requestDTO);
            
            return ResponseEntity.ok(ApiResponse.success("商品发布成功", responseDTO));
            
        } catch (Exception e) {
            log.error("发布商品失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "发布商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取商品详情
     */

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


    @GetMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ItemResponseDTO>> getItemById(@PathVariable Integer itemId) {
        try {
            Item item = redisUtils.getItemDetail(itemId);
            if (item == null) {
            ItemResponseDTO responseDTO = itemService.getItemById(itemId);

                kafkaUtils.sendMessage("ItemDetail", String.valueOf(itemId));
                return ResponseEntity.ok(ApiResponse.success(responseDTO));
            }
            System.out.println("缓存命中：" + itemId);
            ItemResponseDTO responseDTO = buildItemResponseDTO(item);
            return ResponseEntity.ok(ApiResponse.success(responseDTO));

        } catch (Exception e) {
            log.error("获取商品详情失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(404, "商品不存在"));
        }
    }
    
    /**
     * 获取所有商品
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getAllItems() {
        try {
            List<ItemResponseDTO> items = itemService.getAllItems();
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取商品列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取商品列表失败"));
        }
    }
    
    /**
     * 获取可售商品
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getAvailableItems() {
        try {
            List<ItemResponseDTO> items = itemService.getAvailableItems();
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取可售商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取可售商品失败"));
        }
    }
    
    /**
     * 根据卖家ID获取商品
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getItemsBySellerId(@PathVariable Integer sellerId) {
        try {
            List<ItemResponseDTO> items = itemService.getItemsBySellerId(sellerId);
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取卖家商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取卖家商品失败"));
        }
    }
    
    /**
     * 根据分类ID获取商品
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getItemsByCategoryId(@PathVariable Integer categoryId) {
        try {
            List<ItemResponseDTO> items = itemService.getItemsByCategoryId(categoryId);
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取分类商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取分类商品失败"));
        }
    }
    
    /**
     * 更新商品信息
     */
    @PutMapping(value = "/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponseDTO>> updateItem(
            @PathVariable Integer itemId,
            @RequestParam("title") String title,
            @RequestParam("price") String price,
            @RequestParam("category") String category,
            @RequestParam("condition") String condition,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("sellerId") Integer sellerId) {
        
        try {
            // 构建UpdateRequestDTO
            ItemUpdateRequestDTO requestDTO = new ItemUpdateRequestDTO();
            requestDTO.setTitle(title);
            requestDTO.setPrice(new java.math.BigDecimal(price));
            requestDTO.setCategory(category);
            requestDTO.setCondition(condition);
            requestDTO.setDescription(description);
            requestDTO.setImage(image);
            requestDTO.setSellerId(sellerId);
            
            ItemResponseDTO responseDTO = itemService.updateItem(itemId, requestDTO);
            return ResponseEntity.ok(ApiResponse.success("商品更新成功", responseDTO));
            
        } catch (Exception e) {
            log.error("更新商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "更新商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 上下架商品
     */
    @PatchMapping("/{itemId}/availability")
    public ResponseEntity<ApiResponse<Void>> toggleItemAvailability(
            @PathVariable Integer itemId,
            @RequestParam Boolean isAvailable,
            @RequestParam Integer operatorId,
            @RequestParam(required = false) String reason) {
        try {
            // 构建ToggleAvailabilityRequestDTO
            ItemToggleAvailabilityRequestDTO requestDTO = new ItemToggleAvailabilityRequestDTO();
            requestDTO.setIsAvailable(isAvailable);
            requestDTO.setOperatorId(operatorId);
            requestDTO.setReason(reason);
            
            itemService.toggleItemAvailability(itemId, requestDTO);
            String message = requestDTO.getOperationDescription() + "成功";
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (Exception e) {
            log.error("商品上下架失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "操作失败: " + e.getMessage()));
        }
    }
    
    /**
     * 搜索商品（复杂查询）
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponseDTO<ItemResponseDTO>>> searchItems(
            @Valid @RequestBody ItemQueryRequestDTO requestDTO) {
        try {
            PageResponseDTO<ItemResponseDTO> result = itemService.queryItems(requestDTO);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("搜索商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "搜索商品失败"));
        }
    }
    
    /**
     * 分页获取商品列表（用于首页）
     */
//    @GetMapping("/page")
//    public ResponseEntity<ApiResponse<PageResponseDTO<ItemResponseDTO>>> getItemsPage(
//            @RequestParam(defaultValue = "0") Integer page,
//            @RequestParam(defaultValue = "20") Integer size,
//            @RequestParam(defaultValue = "update_time") String sortBy,
//            @RequestParam(defaultValue = "DESC") String sortDirection,
//            @RequestParam(required = false) Integer categoryId,
//            @RequestParam(required = false) String minPrice,
//            @RequestParam(required = false) String maxPrice,
//            @RequestParam(required = false) String condition,
//            @RequestParam(required = false) String keyword) {
//        try {
//            // 构建查询参数
//            ItemQueryRequestDTO requestDTO = new ItemQueryRequestDTO();
//            requestDTO.setPage(page);
//            requestDTO.setSize(size);
//            requestDTO.setSortBy(sortBy);
//            requestDTO.setSortDirection(sortDirection);
//            requestDTO.setIsAvailable(true); // 首页只显示可售商品
//
//            if (categoryId != null) {
//                requestDTO.setCategoryId(categoryId);
//            }
//
//            if (minPrice != null && !minPrice.trim().isEmpty()) {
//                requestDTO.setMinPrice(new java.math.BigDecimal(minPrice));
//            }
//
//            if (maxPrice != null && !maxPrice.trim().isEmpty()) {
//                requestDTO.setMaxPrice(new java.math.BigDecimal(maxPrice));
//            }
//
//            if (condition != null && !condition.trim().isEmpty()) {
//                requestDTO.setCondition(condition);
//            }
//
//            if (keyword != null && !keyword.trim().isEmpty()) {
//                requestDTO.setKeyword(keyword);
//            }
//
//            PageResponseDTO<ItemResponseDTO> result = itemService.queryItems(requestDTO);
//            return ResponseEntity.ok(ApiResponse.success(result));
//        } catch (Exception e) {
//            log.error("获取分页商品失败: {}", e.getMessage());
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error(500, "获取商品列表失败"));
//        }
//    }
    
    // ==================== 收藏相关接口 ====================
    
    /**
     * 检查商品收藏状态
     */
    @GetMapping("/{itemId}/favorite/check")
    public ResponseEntity<ApiResponse<Boolean>> checkItemFavoriteStatus(@PathVariable Integer itemId, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            log.info("检查商品收藏状态: itemId={}, userId={}", itemId, currentUserId);
            boolean isFavorited = itemService.checkItemFavoriteStatus(currentUserId, itemId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", isFavorited));
        } catch (Exception e) {
            log.error("检查收藏状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "检查收藏状态失败"));
        }
    }
    
    /**
     * 获取商品收藏数量
     */
    @GetMapping("/{itemId}/favorite/count")
    public ResponseEntity<ApiResponse<Long>> getItemFavoriteCount(@PathVariable Integer itemId) {
        try {
            log.info("获取商品收藏数量: itemId={}", itemId);
            long count = itemService.getItemFavoriteCount(itemId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", count));
        } catch (Exception e) {
            log.error("获取收藏数量失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取收藏数量失败"));
        }
    }
    
    /**
     * 添加商品收藏
     */
    @PostMapping("/{itemId}/favorite")
    public ResponseEntity<ApiResponse<Void>> addItemFavorite(@PathVariable Integer itemId, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            log.info("添加商品收藏: itemId={}, userId={}", itemId, currentUserId);
            itemService.addItemFavorite(currentUserId, itemId);
            return ResponseEntity.ok(ApiResponse.success("收藏成功", null));
        } catch (Exception e) {
            log.error("添加收藏失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "收藏失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消商品收藏
     */
    @DeleteMapping("/{itemId}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeItemFavorite(@PathVariable Integer itemId, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            log.info("取消商品收藏: itemId={}, userId={}", itemId, currentUserId);
            itemService.removeItemFavorite(currentUserId, itemId);
            return ResponseEntity.ok(ApiResponse.success("取消收藏成功", null));
        } catch (Exception e) {
            log.error("取消收藏失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "取消收藏失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户收藏的商品列表
     */
    @GetMapping("/favorites/user")
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getUserFavoriteItems(HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            log.info("获取用户收藏商品列表: userId={}", currentUserId);
            List<ItemResponseDTO> favoriteItems = itemService.getUserFavoriteItems(currentUserId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", favoriteItems));
        } catch (Exception e) {
            log.error("获取用户收藏商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取收藏商品失败"));
        }
    }
    
    /**
     * 获取收藏某商品的用户列表
     */
    @GetMapping("/{itemId}/favorite/users")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getItemFavoriteUsers(@PathVariable Integer itemId) {
        try {
            log.info("获取商品收藏用户列表: itemId={}", itemId);
            List<UserInfo> favoriteUsers = itemService.getItemFavoriteUsers(itemId);
            return ResponseEntity.ok(ApiResponse.success("查询成功", favoriteUsers));
        } catch (Exception e) {
            log.error("获取商品收藏用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取收藏用户失败"));
        }
    }
    
    // ==================== 点赞相关接口（保留原有功能） ====================
    
    /**
     * 商品点赞
     */
    @PostMapping("/{itemId}/like")
    public ResponseEntity<ApiResponse<Void>> likeItem(@PathVariable Integer itemId) {
        try {
            itemService.likeItem(itemId);
            return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
        } catch (Exception e) {
            log.error("点赞失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "点赞失败"));
        }
    }
    
    /**
     * 取消点赞
     */
    @DeleteMapping("/{itemId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeItem(@PathVariable Integer itemId) {
        try {
            itemService.unlikeItem(itemId);
            return ResponseEntity.ok(ApiResponse.success("取消点赞成功", null));
        } catch (Exception e) {
            log.error("取消点赞失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "取消点赞失败"));
        }
    }
    
    /**
     * 删除商品（支持软删除和硬删除）
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Integer itemId,
            @RequestParam Integer operatorId,
            @RequestParam(defaultValue = "false") Boolean forceDelete) {
        
        try {
            ItemDeleteRequestDTO requestDTO = new ItemDeleteRequestDTO();
            requestDTO.setOperatorId(operatorId);
            requestDTO.setForceDelete(forceDelete);
            
            itemService.deleteItem(itemId, requestDTO);

            String message = forceDelete ? "商品永久删除成功" : "商品删除成功";
            return ResponseEntity.ok(ApiResponse.success(message, null));

        } catch (Exception e) {
            log.error("删除商品失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "删除商品失败: " + e.getMessage()));
        }
    }
    
    /**
<<<<<<< HEAD
     * 获取用户浏览记录统计
=======
     * 恢复已删除的商品
     */
    @PostMapping("/{itemId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreItem(
            @PathVariable Integer itemId,
            @RequestParam Integer operatorId) {
        try {
            itemService.restoreItem(itemId, operatorId);
            return ResponseEntity.ok(ApiResponse.success("商品恢复成功", null));
        } catch (Exception e) {
            log.error("恢复商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "恢复商品失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户的所有商品（包括已删除，用于管理界面）
     */
    @GetMapping("/seller/{sellerId}/all")
    public ResponseEntity<ApiResponse<List<ItemResponseDTO>>> getAllItemsBySellerId(@PathVariable Integer sellerId) {
        try {
            List<ItemResponseDTO> items = itemService.getAllItemsBySellerIdIncludingDeleted(sellerId);
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取用户所有商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取用户所有商品失败"));
        }
    }
    
    /**
     * 复杂查询商品（分页）
>>>>>>> origin/sql
     */
    @GetMapping("/view-records/user")
    public ResponseEntity<ApiResponse<List<Object>>> getUserViewRecords(HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            List<Object> viewRecords = itemService.getUserViewRecords(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(viewRecords));
        } catch (Exception e) {
            log.error("获取用户浏览记录失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取浏览记录失败"));
        }
    }
    
    /**
     * 获取热门分类（基于浏览记录）
     */
    @GetMapping("/view-records/popular-categories")
    public ResponseEntity<ApiResponse<List<Object>>> getPopularCategories() {
        try {
            List<Object> popularCategories = itemService.getPopularCategories();
            return ResponseEntity.ok(ApiResponse.success(popularCategories));
        } catch (Exception e) {
            log.error("获取热门分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取热门分类失败"));
        }
    }

    @GetMapping("/category/{categoryId}/page")
    public ResponseEntity<ApiResponse<PageResponseDTO<ItemResponseDTO>>> getItemsByCategoryPaged(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "update_time") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            PageResponseDTO<ItemResponseDTO> result = itemService.getItemsByCategoryPaged(categoryId, page, size, sortBy, sortDirection);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取分类商品分页失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取分类商品分页失败"));
        }
    }
    
    // ==================== 新增功能 ====================
    
    /**
     * 分页获取商品
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponseDTO<ItemResponseDTO>>> getItemsPage(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "update_time") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String keyword) {
        
        try {
            log.info("分页查询商品: page={}, size={}, sortBy={}, sortDirection={}, categoryId={}, minPrice={}, maxPrice={}, condition={}, keyword={}", 
                    page, size, sortBy, sortDirection, categoryId, minPrice, maxPrice, condition, keyword);
            
            // 构建查询请求
            ItemQueryRequestDTO queryRequest = new ItemQueryRequestDTO();
            queryRequest.setPage(page);
            queryRequest.setSize(size);
            queryRequest.setSortBy(sortBy);
            queryRequest.setSortDirection(sortDirection);
            queryRequest.setCategoryId(categoryId);
            
            if (minPrice != null && !minPrice.isEmpty()) {
                queryRequest.setMinPrice(new BigDecimal(minPrice));
            }
            if (maxPrice != null && !maxPrice.isEmpty()) {
                queryRequest.setMaxPrice(new BigDecimal(maxPrice));
            }
            
            queryRequest.setCondition(condition);
            queryRequest.setKeyword(keyword);
            
            PageResponseDTO<ItemResponseDTO> result = itemService.queryItems(queryRequest);
            
            log.info("分页查询结果: totalElements={}, totalPages={}, currentPage={}", 
                    result.getTotalElements(), result.getTotalPages(), result.getCurrentPage());
            
            return ResponseEntity.ok(ApiResponse.success("查询成功", result));
            
        } catch (Exception e) {
            log.error("分页查询商品失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 智能搜索商品（使用Elasticsearch）
     * 有关键词时使用Elasticsearch智能搜索，仅筛选条件时使用JPA搜索
     * GET /api/items/smart-search?keyword=xxx&categoryId=1&minPrice=10&maxPrice=100&condition=全新&page=0&size=12&sort=price,asc
     */
    @GetMapping("/smart-search")
    public ResponseEntity<ApiResponse<Page<ItemResponseDTO>>> smartSearchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "updateTime,desc") String sort) {
        try {
            // 判断是否有搜索关键词
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            
            if (hasKeyword) {
                // 有关键词：使用Elasticsearch智能搜索（包含筛选条件）
                try {
                    String[] sortParts = sort.split(",");
                    String sortBy = sortParts[0].trim();
                    String sortOrder = sortParts.length > 1 ? sortParts[1].trim() : "desc";
                    
                    List<ItemDocument> results = itemSearchService.smartSearchItems(
                            keyword, categoryId, minPrice, maxPrice, sortBy, sortOrder, page, size);
                    
                    // 转换为ItemResponseDTO并创建Page对象
                    List<ItemResponseDTO> itemDtos = results.stream()
                            .map(this::convertToItemResponseDTO)
                            .collect(Collectors.toList());
                    
                    Pageable pageable = PageRequest.of(page, size);
                    Page<ItemResponseDTO> resultPage = new PageImpl<>(itemDtos, pageable, itemDtos.size());
                    
                    log.info("使用Elasticsearch智能搜索（关键词: {}），返回 {} 个结果", keyword, itemDtos.size());
                    return ResponseEntity.ok(ApiResponse.success(resultPage));
                    
                } catch (Exception esException) {
                    log.error("Elasticsearch搜索失败，回退到JPA搜索: {}", esException.getMessage());
                    // 回退到JPA搜索
                }
            }
            
            // 没有关键词或ES搜索失败：使用JPA搜索进行筛选
            Sort sortObj = parseSort(sort);
            Pageable pageable = PageRequest.of(page, size, sortObj);
            
            // 构建查询参数
            ItemQueryRequestDTO requestDTO = new ItemQueryRequestDTO();
            requestDTO.setPage(page);
            requestDTO.setSize(size);
            requestDTO.setSortBy(sortObj.iterator().next().getProperty());
            requestDTO.setSortDirection(sortObj.iterator().next().getDirection().name());
            requestDTO.setIsAvailable(true); // 只显示可售商品
            requestDTO.setKeyword(keyword);
            requestDTO.setCategoryId(categoryId);
            requestDTO.setMinPrice(minPrice);
            requestDTO.setMaxPrice(maxPrice);
            requestDTO.setCondition(condition);
            
            PageResponseDTO<ItemResponseDTO> pageResult = itemService.queryItems(requestDTO);
            
            // 转换为Page对象
            Page<ItemResponseDTO> items = new PageImpl<>(
                    pageResult.getContent(), 
                    pageable, 
                    pageResult.getTotalElements()
            );
            
            if (hasKeyword) {
                log.info("ES失败回退到JPA搜索，返回 {} 个结果", items.getTotalElements());
            } else {
                log.info("使用JPA筛选搜索（仅筛选条件），返回 {} 个结果", items.getTotalElements());
            }
            
            return ResponseEntity.ok(ApiResponse.success(items));
            
        } catch (Exception e) {
            log.error("智能搜索商品失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "搜索商品失败: " + e.getMessage()));
        }
    }
    
    // ==================== Elasticsearch 同步功能 ====================
    
    /**
     * 全量同步到Elasticsearch
     */
    @PostMapping("/sync-elasticsearch/full")
    public ResponseEntity<ApiResponse<String>> fullSyncToElasticsearch() {
        try {
            if (elasticsearchSyncService == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Elasticsearch同步服务未配置"));
            }
            
            elasticsearchSyncService.fullSyncToElasticsearch()
                    .thenAccept(result -> log.info("全量同步任务完成: {}", result))
                    .exceptionally(throwable -> {
                        log.error("全量同步任务失败: {}", throwable.getMessage());
                        return null;
                    });
            
            return ResponseEntity.ok(ApiResponse.success("全量同步成功", "全量同步任务已启动"));
        } catch (Exception e) {
            log.error("全量同步到Elasticsearch失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "全量同步失败: " + e.getMessage()));
        }
    }
    
    /**
     * 增量同步到Elasticsearch
     */
    @PostMapping("/sync-elasticsearch/incremental")
    public ResponseEntity<ApiResponse<String>> incrementalSyncToElasticsearch() {
        try {
            if (elasticsearchSyncService == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Elasticsearch同步服务未配置"));
            }
            
            elasticsearchSyncService.incrementalSyncToElasticsearch()
                    .thenAccept(result -> log.info("增量同步任务完成: {}", result))
                    .exceptionally(throwable -> {
                        log.error("增量同步任务失败: {}", throwable.getMessage());
                        return null;
                    });
            
            return ResponseEntity.ok(ApiResponse.success("增量同步成功", "增量同步任务已启动"));
        } catch (Exception e) {
            log.error("增量同步到Elasticsearch失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "增量同步失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取同步状态
     */
    @GetMapping("/sync-elasticsearch/status")
    public ResponseEntity<ApiResponse<String>> getSyncStatus() {
        try {
            if (elasticsearchSyncService == null) {
                return ResponseEntity.ok(ApiResponse.success("同步状态", "Elasticsearch服务未配置"));
            }
            
            String status = elasticsearchSyncService.getSyncStatus();
            return ResponseEntity.ok(ApiResponse.success("获取状态成功", status));
        } catch (Exception e) {
            log.error("获取同步状态失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取同步状态失败: " + e.getMessage()));
        }
    }
    
    // ==================== 产品描述生成功能 ====================
    
    /**
     * 生成商品描述
     */
    @PostMapping("/generate-description")
    public ResponseEntity<ApiResponse<String>> generateDescription(@RequestBody Map<String, Object> request) {
        try {
            if (productDescriptionService == null) {
                // 如果服务未配置，返回默认描述
                String title = (String) request.get("title");
                String category = (String) request.get("category");
                String condition = (String) request.get("condition");
                
                String defaultDescription = String.format(
                    "这是一个%s的%s，新旧程度：%s。品质优良，价格实惠，欢迎咨询购买！",
                    category != null ? category : "商品",
                    title != null ? title : "物品",
                    condition != null ? condition : "良好"
                );
                
                return ResponseEntity.ok(ApiResponse.success("描述生成成功", defaultDescription));
            }
            
            String title = (String) request.get("title");
            String category = (String) request.get("category");
            String condition = (String) request.get("condition");
            
            String description = productDescriptionService.generateDescription(title, category, condition);
            return ResponseEntity.ok(ApiResponse.success("描述生成成功", description));
        } catch (Exception e) {
            log.error("生成商品描述失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "生成描述失败: " + e.getMessage()));
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 解析排序参数
     */
    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String property = parts[0];
            String direction = parts.length > 1 ? parts[1] : "asc";
            
            return Sort.by(Sort.Direction.fromString(direction), property);
        } catch (Exception e) {
            log.warn("解析排序参数失败，使用默认排序: {}", e.getMessage());
            return Sort.by(Sort.Direction.DESC, "updateTime");
        }
    }
    
    /**
     * 将ItemDocument转换为ItemResponseDTO
     */
    private ItemResponseDTO convertToItemResponseDTO(ItemDocument document) {
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setItemId(document.getItemId());
        dto.setTitle(document.getTitle());
        dto.setPrice(document.getPrice());
        dto.setCategoryName(document.getCategoryName());
        dto.setCondition(document.getCondition());
        dto.setDescription(document.getDescription());
        dto.setImageUrl(document.getImageUrl());
        dto.setIsAvailable(document.getIsAvailable());
        dto.setLikes(document.getLikes());
        dto.setSellerName(document.getSellerName());
        
        // 转换时间格式：String -> LocalDateTime
        if (document.getUpdateTime() != null && !document.getUpdateTime().trim().isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                dto.setUpdateTime(LocalDateTime.parse(document.getUpdateTime(), formatter));
            } catch (Exception e) {
                log.warn("解析时间失败: {}, 使用当前时间", document.getUpdateTime());
                dto.setUpdateTime(LocalDateTime.now());
            }
        } else {
            dto.setUpdateTime(LocalDateTime.now());
        }
        
        return dto;
    }
} 