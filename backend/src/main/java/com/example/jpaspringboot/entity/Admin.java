/**
 * 管理员实体类
 *
 * 功能概述：
 * • 映射数据库管理员表，存储管理员账户信息
 * • 实现密码安全存储机制（盐值+哈希）
 * • 提供JPA实体映射和持久化支持
 *
 * 安全特性：
 * • 密码哈希存储避免明文密码泄露
 * • 盐值机制增强密码破解难度
 * • 唯一用户名约束防止重复注册
 *
 * 数据库映射：
 * • 主键自增策略适应多数数据库
 * • 用户名唯一索引保证账户唯一性
 * • 盐值和密码哈希分别存储
 */
package com.example.jpaspringboot.entity;

import jakarta.persistence.*;

@Entity // JPA实体注解，指示Hibernate为该类创建数据库表
public class Admin {

    /**
     * 主键标识 - 使用数据库自增策略
     * GenerationType.IDENTITY 依赖数据库自增字段，适用于MySQL等支持自增主键的数据库
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //数据库会在插入新记录时自动生成一个新的ID。这种策略在大多数RDBMS（例如MySQL）中都很常见。
    private Integer id;

    /**
     * 管理员用户名 - 唯一约束确保用户名不重复
     */
    @Column(unique = true)
    private String name;

    /**
     * 密码盐值 - 用于密码哈希计算的随机字符串
     * 每个用户拥有独立盐值，增强密码安全性
     */
    private String salt;//在实践中，通常不会为盐设置数据库层面的唯一性约束。

    /**
     * 密码哈希值 - 存储加密后的密码，非明文密码
     * 结合盐值进行哈希计算，防止彩虹表攻击
     */
    @Column(name = "password_hash")
    private String passwordHash; // 存储哈希值，而不是实际密码

    // ==================== 构造方法 ====================

    /**
     * JPA要求的无参构造方法
     */
    public Admin() {
    }

    /**
     * 创建新管理员的构造方法（ID由数据库自动生成）
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     */
    public Admin(String name, String salt, String passwordHash) {
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    /**
     * 完整参数构造方法（主要用于测试或数据恢复场景）
     * @param id 管理员ID
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     */
    public Admin(int id,String name, String salt, String passwordHash) {
        this.id = id;
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * 设置
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取
     * @return salt
     */
    public String getSalt() {
        return salt;
    }

    /**
     * 设置
     * @param salt
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * 获取
     * @return passwordHash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 设置
     * @param passwordHash
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return "Admin{id = " + id + ", name = " + name + "}";
    }
}
