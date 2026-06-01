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
                .addPathPatterns("/**")                 // 拦截所有请求
                //.excludePathPatterns("/index.html")
                .excludePathPatterns("/sign-up.html")// 排除注册HTML
                .excludePathPatterns("/sign-in.html")// 排除登录HTML
                .excludePathPatterns("/user/login")     // 排除登录api接⼝
                .excludePathPatterns("/user/register")  // 排除注册api接⼝
                .excludePathPatterns("/user/logout")    // 排除退出api接⼝
                .excludePathPatterns("/swagger*/**")    // 排除登录swagger下所有
                .excludePathPatterns("/v3*/**")         // 排除登录v3下所有，与 swagger相关
                .excludePathPatterns("/actuator/**")    // 排除健康检查和监控接口
                .excludePathPatterns("/dist/**")        // 排除所有静态⽂件
                .excludePathPatterns("/image/**")
                .excludePathPatterns("/**.ico")
                .excludePathPatterns("/js/**");



    }
}
