package com.xzy.forum.services;

import com.xzy.forum.model.User;

public interface IUserService {

/**
 * 创建普通用户
 */

    void createnormalUser(User user);

    /**
     * 根据用户名查询用户
     */
    User selectByUserName(String username);

    User login(String username, String password);

    User selectById(Long Id);

    User updateProfile(Long id, User updateUser);

    void changePassword(Long id, String oldPassword, String newPassword, String passwordRepeat);

    void addOneArticleCountById(Long id);

    /**
     * 用户发帖数减少1
     * @param id
     */
    void subOneArticleCountById(Long id);
}
