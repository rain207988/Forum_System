package com.xzy.forum.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
//无参构造器注解
@NoArgsConstructor
public class Article {
    private Long id;

    private Long boardId;

    private Long userId;

    private String title;

    private Integer visitCount;

    private Integer replyCount;

    private Integer likeCount;

    private Byte state;

    private Byte deleteState;

    private Date createTime;

    private Date updateTime;

    private String content;

    @Schema(description = "是否是自己的文章", example = "true")
    private boolean isOwn;

    //关联对象
    private User user;

    //关联对象
    private Board board;

}