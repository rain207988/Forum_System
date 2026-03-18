package com.xzy.forum.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class LoginInterceptor implements HandlerInterceptor {

    // 登录页面的 URL，可以通过配置文件进行设置
    @Value("${forum.login.url}")
    private String deFaultURL;
    /**
        * 预处理方法，在请求处理之前调用
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if(session !=null && session.getAttribute("user") != null) {
            return true;
        }

        // 用户未登录，重定向到登录页面
        deFaultURL = "/" + deFaultURL;
        response.sendRedirect(deFaultURL);
        //终端流程
        return false;

    }
}
