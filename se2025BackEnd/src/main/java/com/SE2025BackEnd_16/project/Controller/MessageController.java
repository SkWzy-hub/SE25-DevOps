package com.SE2025BackEnd_16.project.Controller;

import com.SE2025BackEnd_16.project.KafkaUtils.KafkaUtils;
import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.dto.converter.MessageConverter;
import com.SE2025BackEnd_16.project.dto.request.MessageCreateRequestDTO;
import com.SE2025BackEnd_16.project.dto.response.ApiResponse;
import com.SE2025BackEnd_16.project.dto.response.MessageResponseDTO;
import com.SE2025BackEnd_16.project.entity.Message;
import com.SE2025BackEnd_16.project.entity.UserInfo;
import com.SE2025BackEnd_16.project.repository.MessageRepository;
import com.SE2025BackEnd_16.project.repository.UserInfoRepository;
import com.SE2025BackEnd_16.project.service.MessageService;
import com.SE2025BackEnd_16.project.utils.JwtAuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 留言控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@Validated
@CrossOrigin(origins = "http://localhost:3000") // 限制CORS来源
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private MessageConverter messageConverter;
    @Autowired
    private KafkaUtils kafkaUtils;
    @Autowired
    private JwtAuthHelper jwtAuthHelper;


    /**
     * 获取商品的所有根留言
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<List<MessageResponseDTO>>> getItemMessages(@PathVariable Integer itemId) {
        try {
            log.info("获取商品留言，商品ID: {}", itemId);
            List<Message> messages1 = redisUtils.getRootMessages(itemId);
            if(messages1 != null && !messages1.isEmpty()) {
                System.out.println("缓存命中Message: " + itemId);
                List<MessageResponseDTO> messages = messages1.stream()
                        .map(message -> {
                            String username = userInfoRepository.findById(message.getUserId())
                                    .map(UserInfo::getUsername)
                                    .orElse("未知用户");
                            List<MessageResponseDTO> replies = messageService.getMessageReplies(message.getMessageId());
                            return messageConverter.toResponseDTO(message, username, replies);
                        })
                        .collect(Collectors.toList());
                return ResponseEntity.ok(ApiResponse.success(messages));
            }


            List<MessageResponseDTO> messages = messageService.getRootMessages(itemId);
            if(messages != null && !messages.isEmpty()) {
                System.out.println("缓存未命中Message： " + itemId);
                kafkaUtils.sendMessage("MessageRoot", String.valueOf(itemId));
            }

            return ResponseEntity.ok(ApiResponse.success(messages));
        } catch (Exception e) {
            log.error("获取商品留言失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取留言失败"));
        }
    }
    
    /**
     * 获取留言的所有回复
     */
    @GetMapping("/{messageId}/replies")
    public ResponseEntity<ApiResponse<List<MessageResponseDTO>>> getMessageReplies(@PathVariable Integer messageId) {
        try {
            log.info("获取留言回复，留言ID: {}", messageId);
            List<MessageResponseDTO> replies = messageService.getMessageReplies(messageId);
            return ResponseEntity.ok(ApiResponse.success(replies));
        } catch (Exception e) {
            log.error("获取留言回复失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取回复失败"));
        }
    }
    
    /**
     * 添加留言
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponseDTO>> addMessage(
            @Valid @RequestBody MessageCreateRequestDTO requestDTO,
            HttpServletRequest request) {
        try {
            log.info("添加留言，商品ID: {}, 父留言ID: {}", requestDTO.getItemId(), requestDTO.getParentId());
            
            // 从JWT token中获取当前用户ID
            Integer currentUserId = jwtAuthHelper.getCurrentUserId(request);
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(401, "用户未登录或token无效"));
            }
            
            // 设置用户ID
            requestDTO.setUserId(currentUserId);
            
            MessageResponseDTO responseDTO = messageService.addMessage(requestDTO);
            return ResponseEntity.ok(ApiResponse.success("留言添加成功", responseDTO));
        } catch (Exception e) {
            log.error("添加留言失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "添加留言失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除留言
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Integer messageId) {
        try {
            log.info("删除留言，留言ID: {}", messageId);
            messageService.deleteMessage(messageId);
            return ResponseEntity.ok(ApiResponse.success("留言删除成功", null));
        } catch (Exception e) {
            log.error("删除留言失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "删除留言失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取商品的留言数量
     */
    @GetMapping("/item/{itemId}/count")
    public ResponseEntity<ApiResponse<Long>> getMessageCount(@PathVariable Integer itemId) {
        try {
            log.info("获取商品留言数量，商品ID: {}", itemId);
            long count = messageService.getMessageCount(itemId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("获取留言数量失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(500, "获取留言数量失败"));
        }
    }
} 