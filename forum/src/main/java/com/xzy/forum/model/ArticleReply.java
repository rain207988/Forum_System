package com.xzy.forum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class ArticleReply {
        private Long id;

        //帖子id，关联Article
        private Long articleId;

        //回复的用户编号
        private Long postUserId;

        //忽略，需求楼中楼功能
        private Long replyId;

        private Long replyUserId;

        //回复正文
        private String content;

        //忽略，需求中点赞功能
        private Integer likeCount;

        //状态 0 正常， 1 禁用
        private Byte state;

        //状态 0 正常， 1 禁用
        @JsonIgnore
        private Byte deleteState;

        private Date createTime;

        private Date updateTime;

        private User user;

}
