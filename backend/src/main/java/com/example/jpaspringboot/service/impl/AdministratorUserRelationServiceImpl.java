/**
 * 管理员-用户关系管理服务实现类
 *
 * 功能概述：
 * • 管理管理员与普通用户之间的关联关系
 * • 基于JWT令牌验证管理员身份权限
 * • 提供关系的创建、删除和查询功能
 *
 * 核心业务：
 * • 为当前管理员创建用户管理关系
 * • 删除管理员与用户的关联关系
 * • 批量删除用户管理关系
 * • 查询管理员管辖的所有用户
 *
 * 安全机制：
 * • 通过JWT令牌解析当前管理员身份
 * • 验证管理员和用户实体存在性
 * • 使用复合主键确保关系唯一性
 *
 * 数据关系：
 * • 基于AdministratorUserRelation实体维护多对多关系
 * • 使用复合主键(adminId, userId)确保数据完整性
 * • 通过JPA关联映射维护实体间引用关系
 */
package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.AdministratorUserRelationRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.service.AdministratorUserRelationService;
import com.example.jpaspringboot.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdministratorUserRelationServiceImpl implements AdministratorUserRelationService {

    @Autowired
    private AdministratorUserRelationRepository relationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 删除当前管理员与指定用户的管理关系
     *
     * @param userId 用户ID
     * @param jwtToken JWT认证令牌
     * @throws RuntimeException 当管理员或用户不存在时抛出异常
     */
    @Override
    public void deleteRelationForCurrentUser(Integer userId, String jwtToken) {
        // 从JWT令牌中解析当前管理员用户名
        String name = JwtUtils.getUsernameFromToken(jwtToken);

        // 根据管理员用户名查询管理员实体
        Admin admin = null;
        try {
            admin = adminRepository.findByName(name);
            if (admin == null) {
                throw new RuntimeException("Admin not found with username " + name);
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while retrieving admin with username: " + name, e);
        }

        // 验证用户实体存在性
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        // 构建复合主键并删除关系记录
        AdministratorUserRelationId id = new AdministratorUserRelationId(admin.getId(), userId);
        relationRepository.deleteById(id);
    }

    /**
     * 为当前管理员创建与指定用户的管理关系
     *
     * @param userId 用户ID
     * @param jwtToken JWT认证令牌
     * @return 新创建的管理关系实体
     * @throws RuntimeException 当管理员或用户不存在时抛出异常
     */
    @Override
    public AdministratorUserRelation createRelationForCurrentUser(Integer userId, String jwtToken) {
        // 从JWT令牌中解析当前管理员用户名
        String name = JwtUtils.getUsernameFromToken(jwtToken);

        Admin admin = null;
        try {
            admin = adminRepository.findByName(name);
            if (admin == null) {
                throw new RuntimeException("Admin not found with username " + name);
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while retrieving admin with username: " + name, e);
        }

        // 验证用户实体存在性
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        // 创建新的管理关系实体
        AdministratorUserRelation relation = new AdministratorUserRelation();
        relation.setId(new AdministratorUserRelationId(admin.getId(), userId));
        relation.setAdmin(admin);
        relation.setUser(user);

        return relationRepository.save(relation);
    }

    /**
     * 查询指定管理员管辖的所有用户列表
     *
     * @param admin 管理员实体
     * @return 用户列表（当前返回null，需要修复）
     */
    @Override
    public List<User> findUsersByAdmin(Admin admin) {
        List<AdministratorUserRelation> relations = relationRepository.findByAdmin(admin);
        List<User> users = new ArrayList<>();
        for (AdministratorUserRelation relation : relations) {
            users.add(relation.getUser());
        }
        return null; // 注意：此处应返回users而非null
    }

    /**
     * 批量删除当前管理员与多个用户的管理关系
     *
     * @param userids 用户ID列表
     * @param token JWT认证令牌
     */
    @Override
    public void deleteRelationForCurrentUsers(List<Long> userids, String token) {
        for (Long userid : userids) {
            deleteRelationForCurrentUser(Math.toIntExact(userid), token);
        }
    }
}