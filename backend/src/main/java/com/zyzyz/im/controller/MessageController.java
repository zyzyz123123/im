package com.zyzyz.im.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zyzyz.im.service.MessageService;
import com.zyzyz.im.service.UserService;
import com.zyzyz.im.dto.UserInfoDTO;
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
    
    @Autowired
    private UserService userService;
    
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
    public Result<List<UserInfoDTO>> getOnlineUsers() {
        List<String> onlineUserIds = websocketSessionManager.getOnlineUsers();
        List<UserInfoDTO> onlineUsers = userService.getUserInfoByUserIds(onlineUserIds);
        return Result.success(onlineUsers);
    }

    @GetMapping("/online/check")
    public Result<Boolean> isOnline(@RequestParam String userId) {
        boolean isOnline = websocketSessionManager.isOnline(userId);
        return Result.success(isOnline);
    }
    
    @GetMapping("/contacts")
    public Result<List<UserInfoDTO>> getRecentContacts(@RequestParam String userId) {
        List<String> contactIds = messageService.selectRecentContactsByUserId(userId);
        List<UserInfoDTO> contacts = userService.getUserInfoByUserIds(contactIds);
        return Result.success(contacts);
    }
    
    @GetMapping("/group/history")
    public Result<List<Message>> getGroupMessages(@RequestParam String groupId) {
        List<Message> messages = messageService.selectByGroupId(groupId);
        return Result.success(messages);
    }
    
    @PostMapping("/users/batch")
    public Result<List<UserInfoDTO>> batchGetUserInfo(@RequestBody List<String> userIds) {
        List<UserInfoDTO> users = userService.getUserInfoByUserIds(userIds);
        return Result.success(users);
    }
}
