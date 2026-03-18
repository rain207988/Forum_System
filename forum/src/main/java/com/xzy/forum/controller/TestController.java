package com.xzy.forum.controller;


import com.xzy.forum.exception.ApplicationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "测试接口", description = "用于系统测试和演示的 API 接口")
@RequestMapping("/test")
@RestController
public class TestController {


    @Operation(summary = "Hello 接口", description = "返回问候消息，用于测试 API 连通性")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "请求成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/hello")
    public String hello(
            @Parameter(description = "用户名", example = "张三")
            @RequestParam(value = "name", required = false, defaultValue = "访客") String name) {
        log.info("Received hello request with name: {}", name);
        return "Hello, " + name + "! Welcome to the Forum System!";
    }

    @Operation(summary = "测试异常", description = "抛出运行时异常，用于测试全局异常处理")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "请求成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/exception")
    public String testException() {
        throw new RuntimeException("This is a test exception");
    }

    @Operation(summary = "测试业务异常", description = "抛出业务异常，用于测试全局异常处理")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "请求成功"),
            @ApiResponse(responseCode = "500", description = "业务异常")
    })
    @GetMapping("/applicationException")
    public String testApplicationException() {
        throw new ApplicationException("This is a test application exception");
    }
}