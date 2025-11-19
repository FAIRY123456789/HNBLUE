/**
 * 管理员-用户关联关系实体类
 *
 * 功能概述：
 * • 建立管理员与用户之间的多对多关联关系
 * • 使用复合主键实现关联表实体映射
 * • 支持JPA懒加载优化查询性能
 *
 * 数据库映射：
 * • 对应数据库中的管理员用户关联表
 * • 使用复合主键(admin_id, user_id)作为唯一标识
 * • 通过@ManyToOne建立与Admin和User实体的关联
 *
 * 关联关系：
 * • 多个管理员可以管理多个用户
 * • 多个用户可以被多个管理员管理
 * • 实际业务中体现为权限分配或责任关系
 *
 * 技术特性：
 * • 使用@EmbeddedId实现复合主键
 * • @MapsId注解确保外键与主键字段映射
 * • @JsonIgnoreProperties避免Jackson序列化问题
 * • FetchType.LAZY优化数据加载性能
 */
package com.example.jpaspringboot.entity;

import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
public class AdministratorUserRelation {

    // 复合主键，包含管理员ID和用户ID
    @EmbeddedId
    private AdministratorUserRelationId id;

    // 关联的管理员实体，懒加载模式
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("adminId")
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // 关联的用户实体，懒加载模式
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 默认构造函数，JPA规范要求
     */
    public AdministratorUserRelation() {
    }

    /**
     * 带参数构造函数
     * @param id 复合主键对象
     * @param admin 关联的管理员实体
     * @param user 关联的用户实体
     */
    public AdministratorUserRelation(AdministratorUserRelationId id, Admin admin, User user) {
        this.id = id;
        this.admin = admin;
        this.user = user;
    }

    /**
     * 获取
     * @return id
     */
    public AdministratorUserRelationId getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(AdministratorUserRelationId id) {
        this.id = id;
    }

    /**
     * 获取
     * @return admin
     */
    public Admin getAdmin() {
        return admin;
    }

    /**
     * 设置
     * @param admin
     */
    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    /**
     * 获取
     * @return user
     */
    public User getUser() {
        return user;
    }

    /**
     * 设置
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
    }

    public String toString() {
        return "AdministratorUserRelation{id = " + id + ", admin = " + admin + ", user = " + user + "}";
    }

}