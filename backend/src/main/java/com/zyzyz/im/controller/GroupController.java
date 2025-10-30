package com.zyzyz.im.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyzyz.im.common.Result;
import com.zyzyz.im.dto.ChatMessage;
import com.zyzyz.im.dto.CreateGroupRequest;
import com.zyzyz.im.dto.GroupResponse;
import com.zyzyz.im.manager.WebsocketSessionManager;
import com.zyzyz.im.service.GroupService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@RestController
@RequestMapping("/group")
public class GroupController {
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private WebsocketSessionManager websocketSessionManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 创建群组
     */
    @PostMapping("/create")
    public Result<GroupResponse> createGroup(
            @RequestBody CreateGroupRequest request,
            HttpSession session
    ) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            GroupResponse group = groupService.createGroup(userId, request);
            
            // 通知所有被邀请的成员刷新群组列表
            if (group.getMemberIds() != null) {
                notifyMembersToRefreshGroups(group.getMemberIds(), group.getGroupName());
            }
            
            return Result.success("创建成功", group);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户的所有群组
     */
    @GetMapping("/list")
    public Result<List<GroupResponse>> getUserGroups(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            List<GroupResponse> groups = groupService.getUserGroups(userId);
            return Result.success("查询成功", groups);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户最近有消息的群组
     */
    @GetMapping("/recent")
    public Result<List<GroupResponse>> getRecentGroups(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            List<GroupResponse> groups = groupService.getRecentGroupsWithMessages(userId);
            return Result.success("查询成功", groups);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取群组成员列表
     */
    @GetMapping("/members")
    public Result<List<String>> getGroupMembers(@RequestParam String groupId, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        // 检查用户是否在群组中
        if (!groupService.isUserInGroup(groupId, userId)) {
            return Result.error("您不在该群组中");
        }
        
        try {
            List<String> memberIds = groupService.getGroupMemberIds(groupId);
            return Result.success("查询成功", memberIds);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 添加成员到群组
     */
    @PostMapping("/addMember")
    public Result<String> addMember(
            @RequestParam String groupId,
            @RequestParam String userId,
            HttpSession session
    ) {
        String operatorId = (String) session.getAttribute("userId");
        if (operatorId == null) {
            return Result.error("未登录");
        }
        
        try {
            groupService.addMemberToGroup(groupId, userId, operatorId);
            
            // 通知新成员刷新群组列表
            notifyMemberAdded(userId, groupId);
            
            return Result.success("添加成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("添加失败：" + e.getMessage());
        }
    }
    
    /**
     * 从群组中移除成员
     */
    @PostMapping("/removeMember")
    public Result<String> removeMember(
            @RequestParam String groupId,
            @RequestParam String userId,
            HttpSession session
    ) {
        String operatorId = (String) session.getAttribute("userId");
        if (operatorId == null) {
            return Result.error("未登录");
        }
        
        try {
            groupService.removeMemberFromGroup(groupId, userId, operatorId);
            
            // 通知被移除的成员
            notifyMemberRemoved(userId, groupId);
            
            return Result.success("移除成功", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("移除失败：" + e.getMessage());
        }
    }
    
    /**
     * 通知成员刷新群组列表
     */
    private void notifyMembersToRefreshGroups(List<String> memberIds, String groupName) {
        try {
            ChatMessage notification = ChatMessage.builder()
                    .type("group_created")
                    .message(groupName)
                    .build();
            
            String messageJson = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (String memberId : memberIds) {
                WebSocketSession session = websocketSessionManager.getSession(memberId);
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            System.err.println("通知成员失败：" + e.getMessage());
        }
    }
    
    /**
     * 通知新成员加入群组
     */
    private void notifyMemberAdded(String userId, String groupId) {
        try {
            ChatMessage notification = ChatMessage.builder()
                    .type("member_added")
                    .groupId(groupId)
                    .message("您已被添加到群组")
                    .build();
            
            String messageJson = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(messageJson);
            
            WebSocketSession session = websocketSessionManager.getSession(userId);
            if (session != null && session.isOpen()) {
                session.sendMessage(textMessage);
            }
        } catch (Exception e) {
            System.err.println("通知成员失败：" + e.getMessage());
        }
    }
    
    /**
     * 通知成员被移除
     */
    private void notifyMemberRemoved(String userId, String groupId) {
        try {
            ChatMessage notification = ChatMessage.builder()
                    .type("member_removed")
                    .groupId(groupId)
                    .message("您已被移出群组")
                    .build();
            
            String messageJson = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(messageJson);
            
            WebSocketSession session = websocketSessionManager.getSession(userId);
            if (session != null && session.isOpen()) {
                session.sendMessage(textMessage);
            }
        } catch (Exception e) {
            System.err.println("通知成员失败：" + e.getMessage());
        }
    }
}

