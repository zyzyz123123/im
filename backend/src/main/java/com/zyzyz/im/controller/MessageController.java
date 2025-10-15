package com.zyzyz.im.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zyzyz.im.service.MessageService;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.manager.WebsocketSessionManager;

@RestController
@RequestMapping("/message")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebsocketSessionManager websocketSessionManager;
    
    @GetMapping("/history")
    public List<Message> getMessages(@RequestParam String fromUserId, @RequestParam String toUserId) {
        return messageService.selectByUsers(fromUserId, toUserId);
    }

    @GetMapping("/unread")
    public List<Message> getUnreadMessages(@RequestParam String userId) {
        return messageService.selectUnreadByUserId(userId);
    }

    @GetMapping("/read")
    public void readMessage(@RequestParam String messageId) {
        messageService.updateStatus(messageId, 1);
    }

    @PostMapping("/batchRead")
    public void batchReadMessage(@RequestParam String fromUserId, @RequestParam String toUserId) {
        messageService.batchUpdateStatusByUsers(fromUserId, toUserId);
    }

    @GetMapping("/online/users")
    public List<String> getOnlineUsers() {
        return websocketSessionManager.getOnlineUsers();
    }

    @GetMapping("/online/check")
    public boolean isOnline(@RequestParam String userId) {
        return websocketSessionManager.isOnline(userId);
    }
}
