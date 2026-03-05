package com.xzy.forum.config;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xzy.forum.dao")
public class MybatisConfig {
}
