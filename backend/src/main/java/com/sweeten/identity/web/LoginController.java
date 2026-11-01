package com.sweeten.identity.web;

import com.sweeten.common.web.Result;
import com.sweeten.identity.model.LoginInfo;
import com.sweeten.identity.model.LoginRequest;
import com.sweeten.identity.service.AuthService;
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
@Tag(name = "01-登录认证", description = "员工登录与认证相关接口")
public class LoginController {

    private final AuthService authService;

    // 不加 @LogOperation: 避免操作日志记录明文密码与返回的 JWT 令牌
    @Operation(summary = "员工登录", description = "使用用户名和密码进行登录认证，返回JWT令牌")
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        log.info("登录请求,用户名:{}", req.getUsername());
        LoginInfo loginInfo = authService.login(req.getUsername(), req.getPassword());
        if (loginInfo != null) {
            return Result.success(loginInfo);
        }
        return Result.error("用户名或密码错误");
    }

}
