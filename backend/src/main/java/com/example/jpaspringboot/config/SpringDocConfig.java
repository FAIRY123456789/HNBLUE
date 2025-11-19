/**
 * SpringDoc OpenAPI 配置类
 *
 * 功能概述：
 * • 配置SpringDoc OpenAPI 3.0规范文档生成
 * • 定义API文档的元数据信息和外部文档链接
 * • 替代传统的Swagger2配置，适配Spring Boot 3.x
 *
 * 核心配置：
 * • API基础信息（标题、描述、版本、许可证）
 * • 外部文档引用链接
 * • 自动生成REST接口文档
 *
 * 访问地址：
 * • 本地开发环境：http://localhost:8088/swagger-ui.html
 * • 在线交互测试：支持"Try it out"功能实时调试接口
 *
 * 技术特性：
 * • 自动扫描@Controller、@RestController注解
 * • 解析方法参数和返回值生成文档
 * • 支持OAS 3.0规范，兼容Swagger UI
 *
 * 使用说明：
 * • 前端开发者可通过文档查看接口参数和响应格式
 * • 支持在线测试接口，无需额外API测试工具
 * • 文档与代码同步更新，确保准确性
 */
package com.example.jpaspringboot.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfig {

    /**
     * 配置OpenAPI文档信息
     *
     * @return OpenAPI配置实例，包含API文档的完整定义
     */
    @Bean
    public OpenAPI restfulOpenAPI() {
        return new OpenAPI()
                // API基础信息配置
                .info(new Info().title("HNBLUE API")
                        .description("Welcome to HNBLUE!")
                        .version("v0.0.1")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                // 外部文档链接配置
                .externalDocs(new ExternalDocumentation()
                        .description("SpringDoc HNBLUE Documentation")
                        .url("https://springdoc.org/v2"));
    }

    // 文档访问地址：http://localhost:8088/swagger-ui.html
    // Spring Boot 3.x使用SpringDoc替代过时的Swagger2

    // 前端开发说明：
    // - 每个控制器方法都提供"Try it out"测试功能
    // - 可查看接口所需的请求参数和响应格式
    // - 支持在线调试，功能类似于ApiPost等API测试工具
}