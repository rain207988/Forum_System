package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.MD5Util;
import com.xzy.forum.utils.ServiceValidationUtils;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;


    @Override
    public void createnormalUser(User user) {
        if (user == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL.getCode(), "用户信息不能为空"));
        }

        if (userMapper.selectByUserName(user.getUsername()) != null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_EXISTS));
        }

        initNormalUser(user);

        int row = userMapper.insertSelective(user);
        if (row != 1) {
            log.error("{}，username={}", ResultCode.FAILED_CREATE, user.getUsername());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }

        log.info("新用户插入成功 username={}", user.getUsername());
    }

    @Override
    public User selectByUserName(String username) {
        return userMapper.selectByUserName(username);
    }


    @Override
    public User login(String username, String password) {

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }

        User user = getUserByUsernameOrThrow(username);

        // 校验密码是否正确
        String encryptPassword = MD5Util.md5Salt(password, user.getSalt());
        if (!encryptPassword.equalsIgnoreCase(user.getPassword())) {
            log.error("{}", ResultCode.FAILED_LOGIN);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }

        return user;
    }


    @Override
    public User selectById(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }


    @Override
    public void addOneArticleCountById(Long id) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "userId");
        ServiceValidationUtils.requireNonNull(userMapper.selectByPrimaryKey(id), ResultCode.ERROR_IS_NULL, "userId", id);

        int row = userMapper.increaseArticleCountById(id);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "增加用户发帖数失败", id);
    }

    @Override
    public void subOneArticleCountById(Long id) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "userId");
        ServiceValidationUtils.requireNonNull(userMapper.selectByPrimaryKey(id), ResultCode.ERROR_IS_NULL, "userId", id);

        int row = userMapper.decreaseArticleCountById(id);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "减少用户发帖数失败", id);
    }

    private void initNormalUser(User user) {
        Date now = new Date();
        user.setGender((byte) 0);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setIsAdmin((byte) 0);
        user.setState((byte) 0);
        user.setDeleteState((byte) 0);
        user.setAvatarUrl(null);
        user.setArticleCount(Objects.requireNonNullElse(user.getArticleCount(), 0));
    }

    private User getUserByUsernameOrThrow(String username) {
        User user = userMapper.selectByUserName(username);
        if (user == null) {
            log.info("{}", ResultCode.FAILED_USER_NOT_EXISTS);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }
        return user;
    }
}
