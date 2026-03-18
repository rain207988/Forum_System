package com.xzy.forum.utils;

/**
 * 字符串相关的⼯具类
 *
 * @Author ⽐特就业课
 */
public class StringUtils {
    /**
     * 字符串是否为空
     *
     * @param value 待验证的字符串
     * @return true 为空 false 不为空
     */
    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }
}