package com.SE2025BackEnd_16.project.KafkaUtils;

import com.SE2025BackEnd_16.project.RedisUtils.RedisUtils;
import com.SE2025BackEnd_16.project.entity.Message;
import com.SE2025BackEnd_16.project.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class KafkaMessageConsumer {
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private KafkaUtils kafkaUtils;

    @KafkaListener(topics = "MessageRoot", groupId = "message-cache-group")
    public void handleMessageRoot(String itemIdStr) {
        try {
            System.out.println("收到缓存预热消息RootMessage： " + itemIdStr);
            int itemId = Integer.parseInt(itemIdStr);
            List<Message> rootMessages = messageRepository.findByItemIdAndParentIdAndIsDeletedOrderByReplyTimeDesc(itemId, 0, false);
            redisUtils.cacheRootMessages(itemId, rootMessages);
        } catch (Exception e) {
            // log error
            System.out.println("处理消息错误RootMessage " + itemIdStr);
            kafkaUtils.sendMessage("RootMessage", itemIdStr);
        }
    }

    @KafkaListener(topics = "replyMessage", groupId = "message-cache-group")
    public void handleReplyMessage(String parentIdStr) {
        try {
            System.out.println("收到缓存预热消息ReplyMessage： " + parentIdStr);
            int parentId = Integer.parseInt(parentIdStr);
            List<Message> replies = messageRepository.findByParentIdAndIsDeletedOrderByReplyTimeAsc(parentId, false);
            redisUtils.cacheReplyMessages(parentId, replies);
        } catch (Exception e) {
            System.out.println("处理消息错误ReplyMessage " + parentIdStr);
            kafkaUtils.sendMessage("replyMessage", parentIdStr);
        }
    }

    @KafkaListener(topics = "addMessage", groupId = "message-cache-group")
    public void handleAddMessage(String messageInfo) {
        try {
            System.out.println("收到添加留言消息： " + messageInfo);
            String[] parts = messageInfo.split(",");
            int messageId = Integer.parseInt(parts[0]);
            int itemId = Integer.parseInt(parts[1]);

            
            // 从数据库获取完整留言信息
            Message message = messageRepository.findById(messageId).orElse(null);
            if (message != null) {
                redisUtils.addMessage(itemId, message);
                System.out.println("异步缓存留言成功，ID: " + messageId);
            }
        } catch (Exception e) {
            System.out.println("处理添加留言消息错误: " + messageInfo);
            kafkaUtils.sendMessage("addMessage", messageInfo);
        }
    }

    @KafkaListener(topics = "deleteMessage", groupId = "message-cache-group")
    public void handleDeleteMessage(String messageIdStr) {
        try {
            System.out.println("收到删除留言消息： " + messageIdStr);
            int messageId = Integer.parseInt(messageIdStr);
            
            // 从数据库获取留言信息（用于确定itemId和parentId）
            Message message = messageRepository.findById(messageId).orElse(null);
            if (message != null) {
                redisUtils.deleteMessage(message.getItemId(), messageId, message.getParentId());
                System.out.println("异步删除留言缓存成功，ID: " + messageId);
            }
        } catch (Exception e) {
            System.out.println("处理删除留言消息错误: " + messageIdStr);
            kafkaUtils.sendMessage("deleteMessage", messageIdStr);
        }
    }
}
