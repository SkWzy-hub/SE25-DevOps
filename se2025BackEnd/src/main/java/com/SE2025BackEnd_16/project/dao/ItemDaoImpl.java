package com.SE2025BackEnd_16.project.dao;

import com.SE2025BackEnd_16.project.entity.Item;
import com.SE2025BackEnd_16.project.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 商品数据访问实现类
 */
@Repository
public class ItemDaoImpl implements ItemDao {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Override
    @Transactional
    public Item save(Item item) {
        return itemRepository.save(item);
    }
    
    @Override
    public Optional<Item> findById(Integer itemId) {
        return itemRepository.findById(itemId);
    }
    
    @Override
    public Optional<Item> findByIdIncludingDeleted(Integer itemId) {
        // 使用JPA原生方法，不会被自定义查询过滤
        return Optional.ofNullable(itemRepository.findByItemId(itemId));
    }
    
    @Override
    public List<Item> findAll() {
        return itemRepository.findAll();
    }
    
    @Override
    public List<Item> findAllById(List<Integer> itemIds) {
        return itemRepository.findAllById(itemIds);
    }
    
    @Override
    public List<Item> findBySellerId(Integer sellerId) {
        return itemRepository.findBySellerId(sellerId);
    }
    
    @Override
    public List<Item> findAllBySellerIdIncludingDeleted(Integer sellerId) {
        return itemRepository.findAllBySellerIdIncludingDeleted(sellerId);
    }
    
    @Override
    public List<Item> findByCategoryId(Integer categoryId) {
        return itemRepository.findByCategoryId(categoryId);
    }
    
    @Override
    public List<Item> findByIsAvailable(Boolean isAvailable) {
        return itemRepository.findByIsAvailable(isAvailable);
    }
    
    @Override
    public List<Item> findBySellerIdAndIsAvailable(Integer sellerId, Boolean isAvailable) {
        return itemRepository.findBySellerIdAndIsAvailable(sellerId, isAvailable);
    }

    @Override
    public Page<Item> findAllPaged(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }
    @Override
    public Page<Item> findByIsAvailableTruePaged(Pageable pageable) {
        return itemRepository.findByIsAvailableTrue(pageable);
    }
    @Override
    public Page<Item> findByCategoryIdPaged(Integer categoryId, Pageable pageable) {
        return itemRepository.findByCategoryId(categoryId, pageable);
    }


    @Override
    @Transactional
    public void updateAvailabilityStatus(Integer itemId, Boolean isAvailable) {
        itemRepository.updateAvailabilityStatus(itemId, isAvailable);
    }
    
    @Override
    @Transactional
    public void softDeleteItem(Integer itemId) {
        itemRepository.softDeleteItem(itemId);
    }
    
    @Override
    @Transactional
    public void restoreItem(Integer itemId) {
        itemRepository.restoreItem(itemId);
    }
    
    @Override
    @Transactional
    public void incrementLikes(Integer itemId) {
        itemRepository.incrementLikes(itemId);
    }
    
    @Override
    @Transactional
    public void decrementLikes(Integer itemId) {
        itemRepository.decrementLikes(itemId);
    }
    
    @Override
    @Transactional
    public void deleteById(Integer itemId) {
        itemRepository.deleteById(itemId);
    }
} 