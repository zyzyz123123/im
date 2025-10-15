
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
}