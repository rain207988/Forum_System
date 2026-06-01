package com.xzy.forum.common;

public enum ResultCode {
    SUCCESS(0, "成功"),
    FAILED(1000, "操作失败"),
    FAILED_UNAUTHORIZED(1001, "未授权"),
    FAILED_PARAMS_VALIDATE(1002, "参数校验失败"),
    FAILED_FORBIDDEN(1003, "禁止访问"),
    FAILED_CREATE(1004, "新增失败"),
    FAILED_NOT_EXISTS(1005, "资源不存在"),

    FAILED_USER_EXISTS(1101, "用户已存在"),
    FAILED_USER_NOT_EXISTS(1102, "用户不存在"),
    FAILED_USER_PASSWORD_INVALID(1106, "原密码不正确"),
    FAILED_USER_MESSAGE_SELF(1107, "不能给自己发送私信"),
    FAILED_ARTICLE_NOT_EXISTS(1301, "文章不存在"),
    FAILED_ARTICLE_NOT_OWN(1302,"非本人文章"),
    FAILED_ARTICLE_BANNED(1303,"帖子已封帖"),
    FAILED_MESSAGE_NOT_EXISTS(1401, "站内信不存在"),
    FAILED_MESSAGE_FORBIDDEN(1402, "无权操作该站内信"),


    FAILED_LOGIN(1103, "用户名或密码错误"),
    FAILED_USER_BANNED(1104, "您已被禁言, 请联系管理员, 并重新登录"),
    FAILED_USER_ARTICLE_COUNT(1202,"更新用户文章数量失败"),


    FAILED_TWO_PWD_NOT_SAME(1105, "两次输入的密码不一致"),
    ERROR_SERVICES(2000, "服务器内部错误"),
    ERROR_IS_NULL(2001, "数据为空"),

    FAILED_BOARD_ARTICLE_COUNT(1201,"更新版块文章数量失败"),
    FAILED_BOARD_NOT_EXISTS(1204,"板块不存在"),
    FAILED_BOARD_BANNER(1203,"板块状态异常");







    long code;
    String message;

    ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public long getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "code = " + code + ", message = " + message;
    }
}
