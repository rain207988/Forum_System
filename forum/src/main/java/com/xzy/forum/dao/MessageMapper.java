package com.xzy.forum.dao;

import com.xzy.forum.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    int insert(Message row);

    int insertSelective(Message row);

    Message selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Message row);

    int updateByPrimaryKey(Message row);

    List<Message> selectInboxByReceiveUserId(Long receiveUserId);

    int countUnreadByReceiveUserId(Long receiveUserId);

    Message selectDetailById(Long id);

    int markRead(@Param("id") Long id, @Param("receiveUserId") Long receiveUserId);
}
