package com.zyzyz.im.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.stereotype.Service;

import com.zyzyz.im.mapper.MessageMapper;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.service.MessageService;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void insert(Message message) {
        messageMapper.insert(message);
    }

    @Override
    public Message selectByMessageId(String messageId) {
        return messageMapper.selectByMessageId(messageId);
    }
    
    @Override
    public List<Message> selectByUsers(String fromUserId, String toUserId) {
        return messageMapper.selectByUsers(fromUserId, toUserId);
    }
    
    @Override
    public List<Message> selectUnreadByUserId(String userId) {
        return messageMapper.selectUnreadByUserId(userId);
    }
    
    @Override
    public void updateStatus(String messageId, Integer status) {
        messageMapper.updateStatus(messageId, status);
    }
    
    @Override
    public void batchUpdateStatusByUsers(String fromUserId, String toUserId) {
        messageMapper.batchUpdateStatusByUsers(fromUserId, toUserId);
    }
}
