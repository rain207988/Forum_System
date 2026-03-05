package com.xzy.forum;

import com.xzy.forum.dao.UserMapper;
import com.xzy.forum.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

@SpringBootTest
class ForumApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    public void test() throws Exception {
        System.out.println("dataSource = " + dataSource.getClass());

        System.out.println("connection = " + dataSource.getConnection());


    }

    @Test
    public void test2(){
        System.out.println("这是一个基于springboot+vue的论坛系统");
    }


    @Autowired
    private UserMapper userMapper;

    @Test
    public void testMybatis () {
        User user = userMapper.selectByPrimaryKey(1l);
        System.out.println(user.toString());
        System.out.println(user.getUsername());
    }
}
