package com.xzy.forum.controller;

import com.xzy.forum.common.AppResult;
import com.xzy.forum.common.ResultCode;
import com.xzy.forum.auth.AuthContext;
import com.xzy.forum.auth.JwtAuthenticationService;
import com.xzy.forum.auth.TokenRevocationService;
import com.xzy.forum.config.ForumRateLimitProperties;
import com.xzy.forum.dto.AuthResponse;
import com.xzy.forum.model.User;
import com.xzy.forum.service.RateLimitService;
import com.xzy.forum.services.IUserService;
import com.xzy.forum.utils.ClientIpUtils;
import com.xzy.forum.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等 API")
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtAuthenticationService jwtAuthenticationService;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private ForumRateLimitProperties rateLimitProperties;

    @Operation(summary = "用户注册", description = "注册新用户，需要提供用户名、昵称、密码和确认密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "400", description = "参数错误（用户名、昵称、密码为空或两次密码不一致）"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public AppResult register(
            HttpServletRequest request,
            @Parameter(description = "用户名", required = true, example = "zhangsan")
            @RequestParam String username,
            @Parameter(description = "用户昵称", required = true, example = "张三")
            @RequestParam String nickname,
            @Parameter(description = "登录密码", required = true, example = "123456")
            @RequestParam String password,
            @Parameter(description = "确认密码", required = true, example = "123456")
            @RequestParam String passwordRepeat) {
        rateLimitService.check(
                rateLimitProperties.getRegister(),
                "forum:rate-limit:register:" + ClientIpUtils.resolveClientIp(request),
                "注册请求过于频繁，请稍后再试"
        );

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(nickname) || StringUtils.isEmpty(password)) {
            log.info(ResultCode.FAILED_PARAMS_VALIDATE.toString());
            return AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE);
        }
        if (!password.equals(passwordRepeat)) {
            log.info("两次输入的密码不一致");
            return AppResult.failed(ResultCode.FAILED_TWO_PWD_NOT_SAME);
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setNickname(nickname.trim());
        user.setPassword(password);
        userService.createnormalUser(user);
        return AppResult.success("注册成功", null);
    }

    @Operation(summary = "用户登录", description = "用户登录，需要提供用户名和密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "400", description = "参数错误（用户名或密码为空）"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public AppResult<AuthResponse> login(
            HttpServletRequest request,
            @Parameter(description = "用户名", required = true) @RequestParam("username") String username,
            @Parameter(description = "密码", required = true) @RequestParam("password") String password) {
        rateLimitService.check(
                rateLimitProperties.getLogin(),
                "forum:rate-limit:login:" + ClientIpUtils.resolveClientIp(request),
                "登录请求过于频繁，请稍后再试"
        );
        User user = userService.login(username, password);
        String token = jwtAuthenticationService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(
                user,
                token,
                jwtAuthenticationService.getTokenPrefix().trim(),
                jwtAuthenticationService.getExpiresAt(token)
        );
        return AppResult.success("登录成功", authResponse);
    }

    @RequestMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    public AppResult logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = jwtAuthenticationService.resolveToken(authorizationHeader);
        if (token != null) {
            tokenRevocationService.revoke(token, jwtAuthenticationService.getExpiresAt(token));
        }
        log.info("用户登出成功");
        return AppResult.success("退出成功", null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息")
    public AppResult<User> getUserInfo(Long id) {
        User user;
        if (id == null) {
            user = requireLoginUser();
        } else {
            user = userService.selectById(id);
        }

        if (user == null) {
            log.info(ResultCode.FAILED_USER_NOT_EXISTS.toString());
            return AppResult.failed(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return AppResult.success(user);
    }

    @PostMapping("/modifyInfo")
    public AppResult<User> modifyInfo(@RequestParam(value = "nickname", required = false) String nickname,
                                      @RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "phoneNum", required = false) String phoneNum,
                                      @RequestParam(value = "remark", required = false) String remark,
                                      @RequestParam(value = "avatarUrl", required = false) String avatarUrl) {
        User currentUser = requireLoginUser();

        User updateUser = new User();
        updateUser.setNickname(nickname);
        updateUser.setEmail(email);
        updateUser.setPhoneNum(phoneNum);
        updateUser.setRemark(remark);
        updateUser.setAvatarUrl(avatarUrl);

        User latestUser = userService.updateProfile(currentUser.getId(), updateUser);
        return AppResult.success("资料修改成功", latestUser);
    }

    @PostMapping("/modifyPwd")
    public AppResult modifyPwd(@RequestParam("oldPassword") String oldPassword,
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("passwordRepeat") String passwordRepeat) {
        User currentUser = requireLoginUser();
        userService.changePassword(currentUser.getId(), oldPassword, newPassword, passwordRepeat);
        return AppResult.success("密码修改成功，请重新登录", null);
    }

    private User requireLoginUser() {
        return AuthContext.requireCurrentUser();
    }
}
