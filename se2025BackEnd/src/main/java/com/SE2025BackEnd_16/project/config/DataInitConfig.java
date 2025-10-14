package com.SE2025BackEnd_16.project.config;

import com.SE2025BackEnd_16.project.entity.Category;
import com.SE2025BackEnd_16.project.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 数据初始化配置类
 */
@Slf4j
@Component
public class DataInitConfig implements CommandLineRunner {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // SQL文件已经处理了分类数据初始化，这里跳过避免冲突
        // initCategories();
        log.info("数据初始化完成 - 分类数据由SQL文件处理");
    }
    
    /**
     * 初始化商品分类数据
     */
    private void initCategories() {
        try {
            // 检查是否已有数据
            if (categoryRepository.count() > 0) {
                log.info("分类数据已存在，跳过初始化");
                return;
            }
            
            // 定义分类数据（与前端保持一致）
            List<String> categoryNames = Arrays.asList(
                "books",           // 📚 教材书籍
                "electronics",     // 💻 数码产品
                "clothing",        // 👕 服装配饰
                "sports",          // ⚽ 运动用品
                "home",            // 🏠 生活用品
                "entertainment",   // 🎮 娱乐休闲
                "transport",       // 🚲 交通工具
                "furniture",       // 🛋️ 家具家居
                "baby",            // 👶 母婴用品
                "pets"             // 🐾 宠物用品
            );
            
            // 创建分类实体并保存
            for (String categoryName : categoryNames) {
                Category category = new Category();
                category.setCategoryName(categoryName);
                category.setIcon(getCategoryIcon(categoryName));
                categoryRepository.save(category);
            }
            
            log.info("分类数据初始化完成，共创建 {} 个分类", categoryNames.size());
            
        } catch (Exception e) {
            log.error("分类数据初始化失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取分类图标
     */
    private String getCategoryIcon(String categoryName) {
        switch (categoryName) {
            case "books":
                return "📚";
            case "electronics":
                return "💻";
            case "clothing":
                return "👕";
            case "sports":
                return "⚽";
            case "home":
                return "🏠";
            case "entertainment":
                return "🎮";
            case "transport":
                return "🚲";
            case "furniture":
                return "🛋️";
            case "baby":
                return "👶";
            case "pets":
                return "🐾";
            default:
                return "📦";
        }
    }
    

} 