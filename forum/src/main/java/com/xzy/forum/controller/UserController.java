package com.xzy.forum.controller;


import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.model.User;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.MD5Util;
import com.xzy.forum.utils.StringUtils;
import com.xzy.forum.utils.UUIDUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等 API")
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Operation(summary = "用户注册", description = "注册新用户，需要提供用户名、昵称、密码和确认密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功", 
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = AppResult.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "参数错误（用户名、昵称、密码为空或两次密码不一致）"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public AppResult register(
            @Parameter(description = "用户名", required = true, example = "zhangsan")
            @RequestParam String username,
            
            @Parameter(description = "用户昵称", required = true, example = "张三")
            @RequestParam String nickname,
            
            @Parameter(description = "登录密码", required = true, example = "123456")
            @RequestParam String password,
            
            @Parameter(description = "确认密码", required = true, example = "123456")
            @RequestParam String passwordRepeat) {
        
        if(StringUtils.isEmpty(username)
                || StringUtils.isEmpty(nickname)
                || StringUtils.isEmpty(password)){
            log.info(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(nickname);
        if(!password.equals(passwordRepeat)){
            log.info("两次输入的密码不一致");
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE.getCode(), "两次输入的密码不一致");
        }

       String salt = UUIDUtils.UUID_32();
        String encryptPassword = MD5Util.md5Salt(password, salt);

        user.setPassword(encryptPassword);
        user.setSalt(salt);

        userService.createnormalUser(user);

        //return AppResult.success("注册成功");
        return AppResult.success();
    }

    @Operation(summary = "用户登录", description = "用户登录，需要提供用户名和密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功", 
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = AppResult.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "参数错误（用户名或密码为空）"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })

    @PostMapping("/login")
    public AppResult login (HttpServletRequest request,
                            @Parameter(description = "用户名", required = true) @RequestParam(value = "username") String username,
                            @Parameter(description = "密码", required = true) @RequestParam(value = "password") String password) {
        // ⽤⼾登录
        // 调用Service层的登录方法，返回User对象，校验都在service层完成
        User user = userService.login(username, password);
        // 获取Session
        HttpSession session = request.getSession(true);
        // 把User设置到Session中
        session.setAttribute("user", user);
        // 登录成功响应，返回用户信息
        return AppResult.success(user);
    }

    @Operation(summary = "用户登出", description = "用户登出系统")
    @PostMapping("/logout")
    public AppResult logout(HttpSession session) {
        session.removeAttribute("user");//删除数据
        session.invalidate();//销毁Session
        log.info("用户登出成功");
        return AppResult.success();
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息")
    public AppResult<User> getUserInfo(HttpSession session,Long id) {
        //先判断id是否传入，如果没有传入，则从session中获取当前登录用户的信息
        //如果 id传入了，则根据id查询用户信息并返回
        User user = null;
        if(id == null){

            if(session == null || session.getAttribute("user") == null) {
                log.info("用户未登录，无法获取用户信息");
                return AppResult.failed(ResultCode.FAILED_FORBIDDEN);
            }
             user = (User)session.getAttribute("user");
        }
        else{
             user = userService.selectById(id);
        }

        //做一个简单的判断，如果用户信息不存在，则返回错误提示
        if(user == null){
            log.info(ResultCode.FAILED_USER_NOT_EXISTS.toString());

            //同样也要返回appResult对象，表示请求失败，并且附带错误码和错误信息
            return AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS);
        }

        return AppResult.success(user);//如果用户信息存在，则返回成功的appResult对象，并且把用户信息作为数据返回给前端;

    }

}
