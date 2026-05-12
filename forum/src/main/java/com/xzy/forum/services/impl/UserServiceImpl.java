package com.xzy.forum.services.impl;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.exception.ApplicationException;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.MD5Util;
import com.xzy.forum.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

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

        User existuser = userMapper.selectByUserName(user.getUsername());
        if (existuser != null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_EXISTS));

        }


        user.setGender((byte) 0);


        Date date = new Date();
        user.setCreateTime(date);
        user.setUpdateTime(date);
        user.setIsAdmin((byte) 0);
        user.setState((byte) 0);
        user.setDeleteState((byte) 0);
        user.setAvatarUrl(null);

        int row = userMapper.insertSelective(user);
        if (row != 1) {
            log.error(ResultCode.FAILED_CREATE.toString() + "，user = ", user.getUsername());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }

        log.info("新用户插入成功 username = " + user.getUsername());

    }

    @Override
    public User selectByUserName(String username) {
        return userMapper.selectByUserName(username);
    }


    @Override
    public User login(String username, String password) {

        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }

        User user = userMapper.selectByUserName(username);

        if (user == null) {
            log.info(ResultCode.FAILED_USER_NOT_EXISTS.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS));
        }

        //校验密码是否正确
        String encryptPassword = MD5Util.md5Salt(password, user.getSalt());
        if(!encryptPassword.equalsIgnoreCase(user.getPassword())){
            log.error(ResultCode.FAILED_LOGIN.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_LOGIN));
        }

        return user;
    }


    @Override
    public User selectById(Long Id){
        return userMapper.selectByPrimaryKey(Id);
    }


    @Override
    public void addOneArticleCountById(Long id) {

        if (id == null || id <= 0) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            log.warn(ResultCode.ERROR_IS_NULL.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }

        Integer articleCount = userMapper.selectByPrimaryKey(id).getArticleCount();


        User updateUser = new User();
        updateUser.setId(id);//id一定要设置，否则不知道更新哪条记录
        updateUser.setArticleCount(articleCount+1);
        Integer row = userMapper.updateByPrimaryKeySelective(updateUser);


        if(row != 1){
            log.warn(ResultCode.FAILED.toString() + "收影响的行数不是1");

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED));
        }
    }

    @Override
    public void subOneArticleCountById(Long id) {
        if (id == null || id <= 0) {
            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            log.warn(ResultCode.ERROR_IS_NULL.toString());
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_IS_NULL));
        }


        User updateUser = new User();
        updateUser.setId(id);
        updateUser.setArticleCount(user.getArticleCount()-1);
        if(updateUser.getArticleCount()!=0){
            updateUser.setArticleCount(0);
        }
        Integer row = userMapper.updateByPrimaryKeySelective(updateUser);

        if(row != 1){
        log.warn(ResultCode.FAILED.toString()+"受影响的行数不等于1");
        }
    }
}
