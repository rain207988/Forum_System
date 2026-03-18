package com.xzy.forum.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

@Data
public class User {
    /** 用户ID，主键 */
    private Long id;

    /** 用户名，用于登录 */
    private String username;

    /** 登录密码（加密后存储） */
    @JsonIgnore
    private String password;

    /** 用户昵称，显示在前台 */
    private String nickname;

    /** 手机号码 */
    private String phoneNum;

    /** 电子邮箱 */
    private String email;

    /** 性别：0-未知，1-男，2-女 */
    private Byte gender;

    /** 加密盐值，用于密码加密 */
    @JsonIgnore
    private String salt;

    /** 头像URL地址 */
    private String avatarUrl;

    /** 发表文章数量 */
    private Integer articleCount;

    /** 是否管理员：0-否，1-是 */
    private Byte isAdmin;

    /** 备注信息 */
    private String remark;

    /** 账号状态：0-正常，1-禁用 */
    private Byte state;

    /** 删除状态：0-正常，1-已删除 */
    @JsonIgnore
    private Byte deleteState;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
