package com.SE2025BackEnd_16.project.service;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.SE2025BackEnd_16.project.config.DashScopeConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 商品描述生成服务
 * 支持AI生成（DashScope）和模板生成两种方式
 */
@Slf4j
@Service
public class ProductDescriptionService {
    @Autowired
    private DashScopeConfig dashScopeConfig;

    private static final List<String> DESCRIPTION_TEMPLATES = Arrays.asList(
        "这是一个%s的%s，新旧程度：%s。品质优良，性价比高，非常适合学生使用。因为%s，现在诚心转让。功能完好，外观良好，支持当面验货。有意者请联系！",
        "%s品质的%s转让，状态：%s。物品保养得很好，一直精心使用。由于%s的原因，现低价出售。物超所值，机会难得，欢迎咨询！",
        "出售%s%s一件，新旧程度：%s。这件物品陪伴了我很久，现在因为%s需要转让。功能正常，性能稳定，适合有需要的同学。价格合理，诚信交易！",
        "转让%s的%s，品相：%s。这是我很喜欢的一件物品，因为%s的缘故现在转让。质量可靠，使用体验良好。希望找到真正需要它的人，欢迎联系！"
    );

    private static final List<String> TRANSFER_REASONS = Arrays.asList(
        "升级换新", "不常使用", "搬家需要", "毕业清理", "闲置太久", "功能重复", "预算调整", "换个风格"
    );

    private final Random random = new Random();

    /**
     * 根据商品信息生成描述
     */
    public String generateDescription(String title, String category, String condition) {
        try {
            log.info("生成商品描述: title={}, category={}, condition={}", title, category, condition);
            // 参数验证和默认值设置
            String safeTitle = StringUtils.hasText(title) ? title : "物品";
            String safeCategory = StringUtils.hasText(category) ? category : "商品";
            String safeCondition = StringUtils.hasText(condition) ? condition : "良好";
            // 如果AI功能可用，尝试使用AI生成描述
            if (dashScopeConfig.isAvailable()) {
                try {
                    String aiDescription = generateAIDescription(safeTitle, safeCategory, safeCondition);
                    if (StringUtils.hasText(aiDescription)) {
                        log.info("AI商品描述生成成功，长度: {}", aiDescription.length());
                        return aiDescription;
                    }
                } catch (Exception e) {
                    log.warn("AI生成描述失败，回退到模板生成: {}", e.getMessage());
                }
            }
            // 使用模板生成描述（AI不可用或失败时的回退方案）
            String template = DESCRIPTION_TEMPLATES.get(random.nextInt(DESCRIPTION_TEMPLATES.size()));
            String reason = TRANSFER_REASONS.get(random.nextInt(TRANSFER_REASONS.size()));
            String description = String.format(template, safeCategory, safeTitle, safeCondition, reason);
            log.info("模板商品描述生成成功，长度: {}", description.length());
            return description;
        } catch (Exception e) {
            log.error("生成商品描述失败: {}", e.getMessage(), e);
            return generateDefaultDescription(title, category, condition);
        }
    }

    /**
     * 使用AI生成商品描述（基于DashScope多模态对话）
     */
    private String generateAIDescription(String title, String category, String condition) {
        try {
            log.info("尝试使用DashScope AI生成商品描述: {}", title);
            
            // 检查配置
            if (!StringUtils.hasText(dashScopeConfig.getApiKey())) {
                log.warn("DashScope API Key未配置");
                return null;
            }

            // 创建多模态对话实例
            MultiModalConversation conv = new MultiModalConversation();
            
            // 构建专门针对校园二手交易的提示词
            String prompt = String.format(
                "请为这个校园二手商品生成一段吸引人的商品描述。商品信息：\n" +
                "标题：%s\n" +
                "分类：%s\n" +
                "新旧程度：%s\n\n" +
                "要求：\n" +
                "1. 字数控制在80-120字之间\n" +
                "2. 用中文回答\n" +
                "3. 突出商品的特点和卖点\n" +
                "4. 适合校园二手交易场景\n" +
                "5. 语言亲切自然，符合大学生交流习惯\n" +
                "6. 包含商品状态描述（如成色、功能等）\n" +
                "7. 不要包含价格信息\n" +
                "8. 以第一人称视角描述，如'我的'、'转让'等",
                title, category, condition
            );

            // 构建多模态消息（仅文本，不包含图片）
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("text", prompt)
                    ))
                    .build();

            // 构建请求参数
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeConfig.getApiKey())
                    .model(dashScopeConfig.getModel())
                    .message(userMessage)
                    .build();

            // 调用API
            MultiModalConversationResult result = conv.call(param);
            
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null 
                && !result.getOutput().getChoices().isEmpty()) {
                
                // 获取API返回的content对象
                Object contentObj = result.getOutput().getChoices().get(0).getMessage().getContent();
                String description = parseContentToString(contentObj);
                
                if (StringUtils.hasText(description)) {
                    // 后处理：确保描述适合校园二手交易
                    String processedDescription = postProcessDescription(description);
                    log.info("DashScope AI描述生成成功，描述长度: {}", processedDescription.length());
                    return processedDescription;
                } else {
                    log.error("DashScope API返回内容解析失败");
                    return null;
                }
            } else {
                log.error("DashScope API返回结果为空");
                return null;
            }

        } catch (ApiException e) {
            log.error("DashScope API调用失败: {}", e.getMessage());
            throw new RuntimeException("AI生成描述失败: " + e.getMessage());
        } catch (NoApiKeyException e) {
            log.error("DashScope API Key错误: {}", e.getMessage());
            throw new RuntimeException("AI生成描述失败: " + e.getMessage());
        } catch (UploadFileException e) {
            log.error("DashScope文件上传失败: {}", e.getMessage());
            throw new RuntimeException("AI生成描述失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("AI生成描述失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 解析DashScope API返回的content为纯文本
     * 处理格式: List<Map<String, Object>> 或 String
     */
    @SuppressWarnings("unchecked")
    private String parseContentToString(Object contentObj) {
        try {
            if (contentObj == null) {
                return null;
            }
            
            // 如果直接是字符串，直接返回
            if (contentObj instanceof String) {
                return (String) contentObj;
            }
            
            // 如果是List格式（DashScope API的典型返回格式）
            if (contentObj instanceof List) {
                List<Object> contentList = (List<Object>) contentObj;
                StringBuilder result = new StringBuilder();
                
                for (Object item : contentList) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        // 提取text字段
                        Object textValue = itemMap.get("text");
                        if (textValue != null) {
                            result.append(textValue.toString());
                        }
                    } else if (item != null) {
                        // 其他类型直接转字符串
                        result.append(item.toString());
                    }
                }
                
                return result.toString();
            }
            
            // 其他情况直接转字符串
            return contentObj.toString();
            
        } catch (Exception e) {
            log.error("解析DashScope API返回内容失败，原始内容: {}, 错误: {}", 
                contentObj, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 后处理生成的描述，确保符合校园二手交易特色
     */
    private String postProcessDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        
        String processed = description.trim();
        
        // 移除可能的价格信息
        processed = processed.replaceAll("[￥¥]\\d+", "");
        processed = processed.replaceAll("\\d+元", "");
        processed = processed.replaceAll("价格[^。！？]*[。！？]", "");
        
        // 确保以正确的结尾
        if (!processed.endsWith("。") && !processed.endsWith("！") && !processed.endsWith("？")) {
            processed += "。";
        }
        
        // 添加校园二手交易的常用结尾（如果描述较短）
        if (processed.length() < 80) {
            processed += "诚心转让，有意者请联系！";
        }
        
        return processed;
    }

    /**
     * 生成默认描述
     */
    private String generateDefaultDescription(String title, String category, String condition) {
        String safeTitle = StringUtils.hasText(title) ? title : "物品";
        String safeCategory = StringUtils.hasText(category) ? category : "商品";
        String safeCondition = StringUtils.hasText(condition) ? condition : "良好";
        return String.format(
            "这是一个%s的%s，新旧程度：%s。品质优良，价格实惠，功能完好。" +
            "因为升级换新的原因，现在诚心转让给有需要的同学。" +
            "支持当面验货，确保交易安全。有意者请联系！",
            safeCategory, safeTitle, safeCondition
        );
    }

    /**
     * 根据分类生成特定的描述建议
     */
    public String generateCategorySpecificDescription(String category, String condition) {
        if (!StringUtils.hasText(category)) {
            return generateDefaultDescription("", "", condition);
        }
        String safeCondition = StringUtils.hasText(condition) ? condition : "良好";
        switch (category.toLowerCase()) {
            case "数码产品":
            case "电子设备":
                return String.format("数码产品转让，成色：%s。功能正常，性能稳定，配件齐全。适合学生日常使用，性价比很高。支持现场测试，确保功能完好。诚心出售，价格可议！", safeCondition);
            case "图书教材":
            case "书籍":
                return String.format("图书转让，品相：%s。内容完整，页面清晰，无缺页现象。适合相关专业同学使用，能帮助学习和考试。因为课程结束不再需要，希望物尽其用。价格优惠，欢迎咨询！", safeCondition);
            case "服装配饰":
            case "衣物":
                return String.format("服装转让，成色：%s。款式时尚，尺码合适，面料舒适。平时爱惜使用，保养得很好。因为风格变化现在转让，希望找到合适的新主人。支持试穿，满意再买！", safeCondition);
            case "生活用品":
            case "日用品":
                return String.format("生活用品转让，状态：%s。实用性强，质量可靠，使用方便。是学生宿舍的好帮手，能提高生活品质。因为搬家清理现在出售，价格实惠，机会难得！", safeCondition);
            case "运动器材":
            case "体育用品":
                return String.format("运动器材转让，品相：%s。功能完好，适合日常锻炼使用。能帮助保持身体健康，提高运动体验。因为运动计划调整不再需要，希望转给爱运动的同学。价格合理，欢迎联系！", safeCondition);
            default:
                return String.format("%s转让，新旧程度：%s。品质优良，功能正常，非常实用。精心使用过的好物品，现在诚心转让。适合有需要的同学，价格公道，支持当面交易！", category, safeCondition);
        }
    }
} 