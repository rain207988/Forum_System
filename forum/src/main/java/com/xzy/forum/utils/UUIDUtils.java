package com.xzy.forum.utils;


import java.util.UUID;
/**
 * ⽣成UUID⼯具类
 *
 * @Author ⽐特就业课
 */
public class UUIDUtils {
    /**
     * ⽣成UUID，去除 - 连接字符
     * @return 32位没有 - 字符的UUID
     */
    public static String UUID_32 () {
        return UUID.randomUUID().toString().replace("-", "");
    }
    /**
     * ⽣成36位UUID
     */
    public static String UUID_36 () {
        return UUID.randomUUID().toString();
    }
}