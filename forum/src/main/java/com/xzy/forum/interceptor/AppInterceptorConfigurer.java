package com.xzy.forum.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//表示一个配置类
@Configuration
public class AppInterceptorConfigurer implements WebMvcConfigurer {

    // 注入自定义的登录拦截器
    @Autowired
    private LoginInterceptor appInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(appInterceptor)
                .addPathPatterns("/user/**", "/article/**", "/articles/**", "/reply/**", "/message/**", "/board/**")
                .excludePathPatterns("/user/login")     // 排除登录api接⼝
                .excludePathPatterns("/user/refreshToken")
                .excludePathPatterns("/user/register")  // 排除注册api接⼝
                .excludePathPatterns("/user/logout")    // 排除退出api接⼝
                .excludePathPatterns("/swagger*/**")
                .excludePathPatterns("/v3*/**")
                .excludePathPatterns("/actuator/**");
    }
}
