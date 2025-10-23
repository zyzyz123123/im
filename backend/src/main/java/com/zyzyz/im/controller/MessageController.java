package com.zyzyz.im.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zyzyz.im.service.MessageService;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.manager.WebsocketSessionManager;
import com.zyzyz.im.common.Result;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private WebsocketSessionManager websocketSessionManager;
    
    @GetMapping("/history")
    public Result<List<Message>> getMessages(@RequestParam String fromUserId, @RequestParam String toUserId) {
        List<Message> messages = messageService.selectByUsers(fromUserId, toUserId);
        return Result.success(messages);
    }

    @GetMapping("/unread")
    public Result<List<Message>> getUnreadMessages(@RequestParam String userId) {
        List<Message> messages = messageService.selectUnreadByUserId(userId);
        return Result.success(messages);
    }

    @GetMapping("/read")
    public Result<Void> readMessage(@RequestParam String messageId) {
        messageService.updateStatus(messageId, 1);
        return Result.success();
    }

    @PostMapping("/batchRead")
    public Result<Void> batchReadMessage(@RequestParam String fromUserId, @RequestParam String toUserId) {
        messageService.batchUpdateStatusByUsers(fromUserId, toUserId);
        return Result.success();
    }

    @GetMapping("/online/users")
    public Result<List<String>> getOnlineUsers() {
        List<String> onlineUsers = websocketSessionManager.getOnlineUsers();
        return Result.success(onlineUsers);
    }

    @GetMapping("/online/check")
    public Result<Boolean> isOnline(@RequestParam String userId) {
        boolean isOnline = websocketSessionManager.isOnline(userId);
        return Result.success(isOnline);
    }
    
    @GetMapping("/contacts")
    public Result<List<String>> getRecentContacts(@RequestParam String userId) {
        List<String> contacts = messageService.selectRecentContactsByUserId(userId);
        return Result.success(contacts);
    }
}
