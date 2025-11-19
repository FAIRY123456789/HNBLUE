/**
 * 用户服务接口层
 *
 * 功能概述：
 * • 定义用户管理核心业务操作契约
 * • 提供用户CRUD操作、权限管理和信息查询功能
 * • 支持分页查询和批量操作
 *
 * 核心功能模块：
 * • 用户注册与基本信息管理
 * • 用户信息查询与范围检索
 * • 密码重置与安全验证
 * • 管理员用户管理功能
 * • 当前用户信息获取
 *
 * 设计特点：
 * • 接口与实现分离，支持多数据源适配
 * • 支持分页查询优化大数据量场景
 * • 提供灵活的参数配置（必需/可选参数）
 * • 集成Token身份验证机制
 *
 * 安全考虑：
 * • 密码重置需验证用户身份
 * • 管理员操作需要权限校验
 * • 用户信息更新支持部分字段修改
 */
package com.example.jpaspringboot.service;

import com.example.jpaspringboot.dto.UserInfoDTO;
import com.example.jpaspringboot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

public interface UserService {

    /**
     * 查询指定ID范围内的用户列表
     * @param startId 起始用户ID
     * @param endId 结束用户ID
     * @return 用户实体列表
     */
    List<User> selectUsersInRange(int startId, int endId);

    /**
     * 注册新用户（基础信息）
     * @param name 用户名
     * @param password 密码
     * @return 用户ID
     */
    @ResponseBody int addUser(@RequestParam String name, @RequestParam String password);

    /**
     * 注册新用户（完整信息）
     * @param name 用户名
     * @param password 密码
     * @param email 邮箱地址
     * @param birthdate 出生日期
     * @return 用户ID
     */
    @ResponseBody int addUser(@RequestParam String name, @RequestParam String password,
                              @RequestParam String email, @RequestParam String birthdate);

    /**
     * 更新用户基本信息
     * @param id 用户ID
     * @param name 用户名
     * @param password 密码
     * @return 更新结果
     */
    @ResponseBody boolean updateUser(@RequestParam int id, @RequestParam String name, @RequestParam String password);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    User findByUsername(String username);

    /**
     * 更新用户完整信息（支持部分字段更新）
     * @param id 用户ID
     * @param name 用户名
     * @param password 密码
     * @param email 邮箱地址（可选）
     * @param birthdate 出生日期（可选）
     * @return 更新结果
     */
    @ResponseBody boolean updateUser(@RequestParam int id, @RequestParam String name,
                                     @RequestParam String password,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) String birthdate);

    /**
     * 更新当前登录用户信息
     * @param token 用户身份令牌
     * @param name 用户名
     * @param password 密码
     * @param email 邮箱地址
     * @param birthdate 出生日期
     * @return 更新结果
     */
    boolean updateCurrentUserInfo(String token, String name, String password, String email, String birthdate);

    /**
     * 根据ID删除单个用户
     * @param id 用户ID
     * @return 删除结果
     */
    @ResponseBody boolean deleteUserById(@RequestParam Long id);

    /**
     * 批量删除用户
     * @param ids 用户ID列表
     * @return 删除结果
     */
    @ResponseBody boolean deleteUserByIds(@RequestParam List<Long> ids);

    /**
     * 分页查询管理员管理的用户列表
     * @param token 管理员身份令牌
     * @param pageable 分页参数
     * @return 分页用户数据
     */
    Page<User> findUsersManagedByAdmin(String token, Pageable pageable);

    /**
     * 验证用户身份并重置密码
     * @param username 用户名
     * @param email 邮箱地址
     * @param newpassword 新密码
     * @return 验证和重置结果
     */
    boolean verifyUser(String username, String email, String newpassword);

    /**
     * 重置用户密码
     * @param username 用户名
     * @param newPassword 新密码
     * @return 重置结果
     */
    boolean resetUserPassword(String username, String newPassword);

    /**
     * 获取当前登录用户信息
     * @param token 用户身份令牌
     * @return 用户信息数据传输对象
     */
    UserInfoDTO getCurrentUserInfo(String token);
}