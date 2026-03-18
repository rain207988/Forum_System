package com.xzy.forum.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 文章视图对象，用于前端列表展示
 * 包含联表查询的作者昵称和版块名称
 */
@Data
public class ArticleVO {
    private Long id;
    private Long boardId;
    private Long userId;
    private String title;
    private Integer visitCount;
    private Integer replyCount;
    private Integer likeCount;
    private Byte state;
    private Date createTime;
    private Date updateTime;
    private String content;

    /** 作者昵称（来自 t_user 表） */
    private String authorNickname;

    /** 版块名称（来自 t_board 表） */
    private String boardName;
}
