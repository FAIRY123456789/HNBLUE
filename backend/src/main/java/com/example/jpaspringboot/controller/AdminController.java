/**
 * 管理员用户管理控制器
 *
 * 功能概述：
 * • 提供管理员对用户数据的增删改查操作接口
 * • 支持用户批量操作和分页查询
 * • 集成权限验证和用户-管理员关系管理
 *
 * 权限控制：
 * • 基于JWT令牌验证管理员身份
 * • 通过用户-管理员关系表管理数据权限
 * • 支持跨管理员的数据隔离访问
 *
 * 核心功能：
 * • 用户信息新增、修改、删除
 * • 批量用户删除操作
 * • 分页用户列表查询
 * • 关键词搜索用户数据
 *
 * 数据流程：
 * • 用户操作同时维护用户表和关系表
 * • 查询时自动过滤当前管理员权限范围内的用户
 * • 异常通过全局异常处理器统一处理
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.dto.UserDTO;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.service.impl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")  // 管理员功能统一前缀
public class AdminController {

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private AdminServiceImpl adminServiceImpl;

    @Autowired
    private AdministratorUserRelationServiceImpl administratorUserRelationServiceImpl;

    /**
     * 服务健康检查接口
     * @return 固定响应字符串
     */
    @GetMapping("/hello")
    public String hello(){
        return "nihao";
    }

    /**
     * 新增用户信息
     * 同时创建用户记录和用户-管理员关联关系
     *
     * @param userDTO 用户数据传输对象
     * @param token JWT认证令牌
     * @return 新增用户的ID
     */
    @PostMapping("/add")
    public ResponseEntity<?> addNewUser(@RequestBody UserDTO userDTO, @RequestHeader("Authorization") String token) {
        // 创建用户基础信息
        int id = userServiceImpl.addUser(
                userDTO.getName(),
                userDTO.getPassword(),
                userDTO.getEmail(),
                userDTO.getBirthdate()
        );
        // 建立用户与当前管理员的关联关系
        administratorUserRelationServiceImpl.createRelationForCurrentUser(id, token);
        // 返回新创建用户的ID
        return ResponseEntity.ok().body(id);
    }

    /**
     * 更新用户信息
     *
     * @param userDTO 用户数据传输对象（包含更新数据）
     * @return 操作结果响应
     */
    @PutMapping("/update")
    public @ResponseBody ResponseEntity<?> updateUser(@RequestBody UserDTO userDTO) {
        System.out.println("收到用户更新请求");
        boolean isUpdated = userServiceImpl.updateUser(
                userDTO.getId(),
                userDTO.getName(),
                userDTO.getPassword(),
                userDTO.getEmail(),
                userDTO.getBirthdate()
        );
        if (isUpdated) {
            return ResponseEntity.ok().body("用户信息更新成功！");
        } else {
            return ResponseEntity.badRequest().body("用户信息更新失败！");
        }
    }

    /**
     * 删除单个用户
     * 先删除用户-管理员关系，再删除用户记录
     *
     * @param id 用户ID
     * @param token JWT认证令牌
     * @return 操作结果响应
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            // 先删除关联关系，再删除用户记录
            administratorUserRelationServiceImpl.deleteRelationForCurrentUser(Math.toIntExact(id), token);
            userServiceImpl.deleteUserById(id);
            return ResponseEntity.ok().body("用户删除成功！");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到ID为 " + id + " 的用户");
        }
    }

    /**
     * 批量删除用户
     * 处理JSON数组格式的用户ID列表
     *
     * @param ids 用户ID列表
     * @param token JWT认证令牌
     * @return 批量操作结果响应
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUsers(@RequestBody List<Long> ids, @RequestHeader("Authorization") String token) {
        System.out.println("执行批量用户删除操作");
        try {
            // 批量删除关联关系和用户记录
            administratorUserRelationServiceImpl.deleteRelationForCurrentUsers(ids, token);
            userServiceImpl.deleteUserByIds(ids);
            return ResponseEntity.ok("用户批量删除成功！");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("部分或全部用户ID未找到");
        }
    }

    /**
     * 分页查询当前管理员管理的用户列表
     *
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param token JWT认证令牌
     * @return 分页用户数据
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsersByAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        // 创建分页请求，按ID升序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> userPage = userServiceImpl.findUsersManagedByAdmin(token, pageable);
        return ResponseEntity.ok().body(userPage);
    }

    /**
     * 用户搜索功能
     * 支持按关键词搜索当前管理员管理的用户
     *
     * @param page 页码（从0开始）
     * @param size 每页记录数
     * @param keyword 搜索关键词
     * @param token JWT认证令牌
     * @return 分页搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestHeader("Authorization") String token) {
        // 创建分页请求，按ID升序排列
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<User> resultPage = userServiceImpl.searchUsersByAdmin(token, keyword, pageable);
        return ResponseEntity.ok(resultPage);
    }
}