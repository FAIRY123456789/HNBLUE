/**
 * 管理员-用户关系管理服务接口
 *
 * 功能概述：
 * • 管理系统管理员与普通用户之间的关联关系
 * • 提供关联关系的创建、查询和删除操作
 * • 支持基于JWT令牌的权限验证
 *
 * 核心业务：
 * • 为当前管理员创建用户关联关系
 * • 删除指定用户的关联关系
 * • 批量删除用户关联关系
 * • 查询管理员管辖的所有用户
 *
 * 权限控制：
 * • 基于JWT令牌验证操作权限
 * • 确保管理员只能操作自己管辖的用户
 * • 防止越权访问和数据泄露
 *
 * 使用场景：
 * • 管理员分配管辖用户
 * • 用户权限管理
 * • 组织结构关系维护
 */
package com.example.jpaspringboot.service;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;

import java.util.List;

public interface AdministratorUserRelationService {

    /**
     * 删除当前管理员与指定用户的关联关系
     * @param userId 用户ID
     * @param jwtToken JWT认证令牌
     */
    void deleteRelationForCurrentUser(Integer userId, String jwtToken);

    /**
     * 为当前管理员创建与指定用户的关联关系
     * @param userId 用户ID
     * @param jwtToken JWT认证令牌
     * @return 创建的关联关系实体
     */
    AdministratorUserRelation createRelationForCurrentUser(Integer userId, String jwtToken);

    /**
     * 查询指定管理员管辖的所有用户列表
     * @param admin 管理员实体
     * @return 用户实体列表
     */
    List<User> findUsersByAdmin(Admin admin);

    /**
     * 批量删除当前管理员与多个用户的关联关系
     * @param userids 用户ID列表
     * @param token JWT认证令牌
     */
    void deleteRelationForCurrentUsers(List<Long> userids, String token);
}