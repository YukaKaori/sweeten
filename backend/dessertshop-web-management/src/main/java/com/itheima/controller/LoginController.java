package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Emp;
import com.itheima.pojo.LoginInfo;
import com.itheima.pojo.LoginRequest;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "01-登录认证", description = "用户登录与认证相关接口")
public class LoginController {

    private final EmpService empService;

    @LogOperation
    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录认证，返回JWT令牌")
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        log.info("登录请求,用户名:{}", req.getUsername());
        Emp emp = new Emp();
        emp.setUsername(req.getUsername());
        emp.setPassword(req.getPassword());
        LoginInfo loginInfo = empService.login(emp);
        if (loginInfo != null) {
            return Result.success(loginInfo);
        }
        return Result.error("用户名或密码错误");
    }

}
