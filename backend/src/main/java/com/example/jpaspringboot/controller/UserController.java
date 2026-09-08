/**
 * 用户信息管理控制器
 *
 * 功能概述：
 * • 提供用户个人信息查询、更新和头像管理接口
 * • 基于JWT令牌进行用户身份认证和授权
 * • 支持用户基本信息和头像的完整管理功能
 *
 * 接口列表：
 * • GET /user/info - 获取当前用户完整信息
 * • POST /user/avatar - 上传并更新用户头像
 * • PUT /user/updateInfo - 更新用户基本信息
 *
 * 安全机制：
 * • 基于Bearer Token的身份验证
 * • 令牌解析获取用户身份信息
 * • 权限校验确保数据隔离
 *
 * 数据流程：
 * • 从Authorization头提取JWT令牌
 * • 解析令牌获取用户ID和权限
 * • 调用服务层处理业务逻辑
 * • 返回标准化响应结果
 */
package com.example.jpaspringboot.controller;

import com.example.jpaspringboot.dto.UserInfoDTO;
import com.example.jpaspringboot.service.impl.UserServiceImpl;
import com.example.jpaspringboot.service.impl.UserActivityLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController // 标识为RESTful控制器，自动序列化返回对象为JSON
@RequestMapping("/user") // 定义控制器基础路径为/user
public class UserController {

    @Autowired
    private UserServiceImpl userServiceImpl; // 用户服务实现类依赖注入

    @Autowired
    private UserActivityLogServiceImpl activityLogService;

    /**
     * 获取当前用户信息接口
     *
     * @param authHeader 包含Bearer Token的认证头
     * @return 用户信息DTO或错误消息
     */
    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 验证Authorization头格式
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7); // 提取JWT令牌（去除"Bearer "前缀）
                UserInfoDTO userInfo = userServiceImpl.getCurrentUserInfo(token); // 调用服务层获取用户信息
                return ResponseEntity.ok(userInfo); // 返回200 OK及用户信息
            } catch (Exception e) {
                System.out.println("Error fetching user info: " + e.getMessage()); // 记录错误日志
                return ResponseEntity.badRequest().body("Unable to fetch user information"); // 返回400错误
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Authorization header"); // 返回认证头格式错误
        }
    }

    /**
     * 上传用户头像接口
     *
     * @param authHeader 包含Bearer Token的认证头
     * @param file 上传的头像文件
     * @return 头像二进制数据或错误消息
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                          @RequestParam("file") MultipartFile file) {

        System.out.println("我已收到头像"); // 调试日志

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7); // 提取JWT令牌
                int userId = userServiceImpl.getUserIDByToken(token); // 从令牌解析用户ID
                // 存储头像并获取头像二进制数据
                byte[] avatarData = userServiceImpl.updateAvatar((long) userId, file);
                activityLogService.record(userId, userServiceImpl.getUsernameById(userId), "User", userId, "USER_AVATAR_UPDATE", "用户修改头像", "/user/avatar", "SUCCESS", null);

                System.out.println("我已修改头像"); // 调试日志

                // 返回头像数据，设置Content-Type为PNG格式
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG) // 设置响应内容类型为PNG
                        .body(avatarData); // 返回头像二进制数据
            } catch (Exception e) {
                // 返回500服务器内部错误
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update avatar: " + e.getMessage());
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Authorization header"); // 返回认证头格式错误
        }
    }

    /**
     * 更新用户基本信息接口
     *
     * @param token 包含Bearer Token的认证头
     * @param payload 包含更新字段的请求体
     * @return 操作结果消息
     */
    @PutMapping("/updateInfo")
    public ResponseEntity<?> updateUserInfo(@RequestHeader(value = "Authorization", required = false) String token,
                                            @RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String password = payload.get("password");
        String email = payload.get("email");
        String birthdate = payload.get("birthdate");

        try {
            boolean success = userServiceImpl.updateCurrentUserInfo(token, name, password, email, birthdate);
            if (success) {
                int userId = userServiceImpl.getUserIDByToken(token.replace("Bearer ", ""));
                activityLogService.record(userId, userServiceImpl.getUsernameById(userId), "User", userId, "USER_PROFILE_UPDATE", "用户修改个人资料", "/user/updateInfo", "SUCCESS", null);
                return ResponseEntity.ok("信息更新成功！");
            }
            return ResponseEntity.badRequest().body("未修改任何内容或更新失败。");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("请确保新密码不少于 8 位，并同时包含英文字母和数字。");
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body("用户名或邮箱已存在，请更换后重试");
        }
    }

}
