/**
 * 管理员服务接口
 *
 * 功能概述：
 * • 定义管理员相关的业务操作契约
 * • 提供测试数据生成和管理员账户管理功能
 *
 * 核心操作：
 * • generateAndSaveTestAdmins - 生成并保存测试管理员数据
 * • addAdmin - 添加新的管理员账户
 *
 * 接口特性：
 * • 使用Spring MVC注解定义Web接口
 * • 支持请求参数自动绑定
 * • 返回JSON格式响应数据
 *
 * 使用场景：
 * • 系统初始化时生成测试管理员
 * • 管理员账户的创建和维护
 * • 权限管理系统的基础服务
 */
package com.example.jpaspringboot.service;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface AdminService {

    /**
     * 生成并保存测试管理员数据
     * 用于系统初始化或测试环境数据准备
     */
    void generateAndSaveTestAdmins();

    /**
     * 添加新的管理员账户
     *
     * @param name 管理员用户名
     * @param password 管理员密码
     * @return 操作结果状态码
     */
    @ResponseBody
    int addAdmin(@RequestParam String name, @RequestParam String password);
}