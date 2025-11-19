/**
 * 用户实体类 - 映射数据库用户表
 *
 * 功能概述：
 * • 定义用户核心数据结构和数据库映射关系
 * • 提供安全的密码存储机制（哈希值+盐值）
 * • 支持用户头像二进制数据存储
 *
 * 安全特性：
 * • 密码哈希存储避免明文泄露
 * • 盐值增强密码哈希安全性
 * • 唯一性约束防止数据重复
 *
 * 数据约束：
 * • 用户名唯一性约束
 * • 邮箱唯一性且非空约束
 * • 出生日期非空约束
 * • 自动生成主键标识
 */
package com.example.jpaspringboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity // 标识为JPA实体类，对应数据库表
public class User {

    /**
     * 用户主键标识
     * GenerationType.IDENTITY - 使用数据库自增主键策略
     * 适用于MySQL等支持自增主键的数据库
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //数据库会在插入新记录时自动为你生成一个新的ID。这种策略在大多数RDBMS（例如MySQL）中都很常见。
    private Integer id;

    @Column(unique = true)
    private String name;

    private String salt;  //在实践中，通常不会为盐设置数据库层面的唯一性约束。
    private String passwordHash;  // 存储哈希值，而不是实际密码

    @Column(unique = true, nullable = false)
    private String email;  // 新增电子邮箱字段，不允许为空，且唯一

    @Column(nullable = false)
    private String birthdate;  // 新增出生日期字段，不允许为空

    @Lob
    @Column(name = "avatar",columnDefinition = "LONGBLOB")
    private byte[] avatar;  // 用于存储图片的二进制数据


    // ==================== 构造方法 ====================

    /**
     * 默认无参构造方法 - JPA规范要求
     */
    public User() {
    }

    /**
     * 基础用户信息构造方法
     * 适用于用户注册场景，ID由数据库自动生成
     *
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     */
    public User(String name, String salt, String passwordHash) {
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    /**
     * 包含ID的用户构造方法
     * 适用于数据导入或测试场景
     *
     * @param id 用户ID
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     */
    public User(int id,String name, String salt, String passwordHash) {
        this.id = id;
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    /**
     * 完整用户信息构造方法
     * 包含邮箱和出生日期信息
     *
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     * @param email 电子邮箱
     * @param birthdate 出生日期
     */
    public User(String name, String salt, String passwordHash, String email, String birthdate) {
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.email = email;
        this.birthdate = birthdate;
    }

    /**
     * 全字段用户构造方法
     * 包含所有用户属性信息
     *
     * @param id 用户ID
     * @param name 用户名
     * @param salt 密码盐值
     * @param passwordHash 密码哈希值
     * @param email 电子邮箱
     * @param birthdate 出生日期
     * @param avatar 头像二进制数据
     */
    public User(Integer id, String name, String salt, String passwordHash, String email, String birthdate, byte[] avatar) {
        this.id = id;
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.email = email;
        this.birthdate = birthdate;
        this.avatar = avatar;
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

    /**
     * 获取
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取
     * @return birthdate
     */
    public String getBirthdate() {
        return birthdate;
    }

    /**
     * 设置
     * @param birthdate
     */
    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    /**
     * 获取
     * @return avatar
     */
    public byte[] getAvatar() {
        return avatar;
    }

    /**
     * 设置
     * @param avatar
     */
    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public String toString() {
        return "User{id = " + id + ", name = " + name + ", salt = " + salt + ", passwordHash = " + passwordHash + ", email = " + email + ", birthdate = " + birthdate + ", avatar = " + avatar + "}";
    }
}