package com.sweeten.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("喜甜 SWEETEN API")
                        .version("1.0.0")
                        .description("喜甜 SWEETEN（甜品 · 烘焙 · 饮品）线上商城与门店运营接口：门店工作台（员工，需 JWT）与顾客商城（/mobile/**，公开）")
                        .contact(new Contact()
                                .name("喜甜 SWEETEN")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("token")
                                .description("JWT 认证令牌，登录成功后返回。格式: <token>")));
    }
}
