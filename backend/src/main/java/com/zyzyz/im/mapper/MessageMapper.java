package com.zyzyz.im.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.zyzyz.im.entity.Message;

@Mapper 
public interface MessageMapper {
    void insert(Message message);
    Message selectByMessageId(String messageId);
    List<Message> selectByUsers(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);
    List<Message> selectUnreadByUserId(String userId);
    void updateStatus(@Param("messageId") String messageId, @Param("status") Integer status);
    void batchUpdateStatusByUsers(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);
    
    /**
     * 查询用户的最近联系人列表（和该用户聊过天的所有人）
     */
    List<String> selectRecentContactsByUserId(String userId);
    
    /**
     * 查询群聊历史消息
     */
    List<Message> selectByGroupId(@Param("groupId") String groupId);
    
    /**
     * 查询所有消息（用于同步到 ES）
     */
    List<Message> selectAll();
}