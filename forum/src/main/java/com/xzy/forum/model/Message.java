package com.xzy.forum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class Message {
    private Long id;

    private Long postUserId;

    private Long receiveUserId;

    private String content;

    private Byte state;

    @JsonIgnore
    private Byte deleteState;

    private Date createTime;

    private Date updateTime;

    private User postUser;

    private User receiveUser;
}
