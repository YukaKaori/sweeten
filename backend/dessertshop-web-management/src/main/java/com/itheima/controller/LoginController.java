package com.itheima.controller;

import com.itheima.annotation.LogOperation;
import com.itheima.pojo.Emp;
import com.itheima.pojo.LoginInfo;
import com.itheima.pojo.LoginRequest;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final EmpService empService;

    @LogOperation
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
