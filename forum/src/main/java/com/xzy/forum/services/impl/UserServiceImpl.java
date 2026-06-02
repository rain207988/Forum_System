package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.PasswordUtils;
import com.xzy.forum.utils.ServiceValidationUtils;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (StringUtils.isEmpty(user.getUsername()) || StringUtils.isEmpty(user.getNickname()) || StringUtils.isEmpty(user.getPassword())) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        if (userMapper.selectByUserName(user.getUsername()) != null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_EXISTS));
        }

        initNormalUser(user);
        user.setPassword(PasswordUtils.hash(user.getPassword()));
        user.setSalt(null);

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
        if (!PasswordUtils.matches(password, user)) {
            log.error("{}", ResultCode.FAILED_LOGIN);
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }

        if (PasswordUtils.isLegacyPassword(user)) {
            upgradeLegacyPassword(user.getId(), password);
            user = userMapper.selectByPrimaryKey(user.getId());
        }
        return user;
    }

    @Override
    @Cacheable(cacheNames = "users", key = "#id", unless = "#result == null")
    public User selectById(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", key = "#id"),
            @CacheEvict(cacheNames = "articleLists", allEntries = true)
    })
    public User updateProfile(Long id, User updateUser) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "userId");
        User currentUser = ServiceValidationUtils.requireNonNull(userMapper.selectByPrimaryKey(id), ResultCode.FAILED_USER_NOT_EXISTS, "userId", id);
        if (updateUser == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        User patchUser = new User();
        patchUser.setId(id);
        patchUser.setUpdateTime(new Date());
        boolean changed = false;

        if (!StringUtils.isEmpty(updateUser.getNickname())) {
            patchUser.setNickname(updateUser.getNickname().trim());
            changed = true;
        }
        if (!StringUtils.isEmpty(updateUser.getEmail())) {
            patchUser.setEmail(updateUser.getEmail().trim());
            changed = true;
        }
        if (!StringUtils.isEmpty(updateUser.getPhoneNum())) {
            patchUser.setPhoneNum(updateUser.getPhoneNum().trim());
            changed = true;
        }
        if (!StringUtils.isEmpty(updateUser.getRemark())) {
            patchUser.setRemark(updateUser.getRemark().trim());
            changed = true;
        }
        if (!StringUtils.isEmpty(updateUser.getAvatarUrl())) {
            patchUser.setAvatarUrl(updateUser.getAvatarUrl().trim());
            changed = true;
        }

        if (!changed) {
            return currentUser;
        }

        int row = userMapper.updateByPrimaryKeySelective(patchUser);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "用户资料更新失败", id);
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", key = "#id"),
            @CacheEvict(cacheNames = "articleLists", allEntries = true)
    })
    public void changePassword(Long id, String oldPassword, String newPassword, String passwordRepeat) {
        ServiceValidationUtils.requirePositiveId(id, ResultCode.FAILED_PARAMS_VALIDATE, "userId");
        if (StringUtils.isEmpty(oldPassword) || StringUtils.isEmpty(newPassword) || StringUtils.isEmpty(passwordRepeat)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        if (!newPassword.equals(passwordRepeat)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_TWO_PWD_NOT_SAME));
        }

        User user = ServiceValidationUtils.requireNonNull(userMapper.selectByPrimaryKey(id), ResultCode.FAILED_USER_NOT_EXISTS, "userId", id);
        if (!PasswordUtils.matches(oldPassword, user)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_PASSWORD_INVALID));
        }

        User patchUser = new User();
        patchUser.setId(id);
        patchUser.setPassword(PasswordUtils.hash(newPassword));
        patchUser.setSalt(null);
        patchUser.setUpdateTime(new Date());
        int row = userMapper.updateByPrimaryKeySelective(patchUser);
        ServiceValidationUtils.requireAffectedOneRow(row, ResultCode.FAILED, "修改密码失败", id);
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
        user.setRemark(Objects.requireNonNullElse(user.getRemark(), "这个人很神秘，还没有填写个人简介"));
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

    private void upgradeLegacyPassword(Long userId, String rawPassword) {
        User patchUser = new User();
        patchUser.setId(userId);
        patchUser.setPassword(PasswordUtils.hash(rawPassword));
        patchUser.setSalt(null);
        patchUser.setUpdateTime(new Date());
        userMapper.updateByPrimaryKeySelective(patchUser);
    }
}
