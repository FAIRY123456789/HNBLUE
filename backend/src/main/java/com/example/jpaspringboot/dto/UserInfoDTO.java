/**
 * 用户信息数据传输对象
 *
 * 功能概述：
 * • 封装用户基本信息用于前后端数据交互
 * • 作为API接口的请求/响应数据载体
 * • 提供用户个人资料的标准化数据结构
 *
 * 数据字段说明：
 * • name - 用户姓名
 * • email - 用户邮箱地址
 * • birthdate - 用户出生日期（字符串格式简化处理）
 * • avatar - 用户头像URL或路径
 *
 * 使用场景：
 * • 用户个人信息查询接口响应
 * • 用户资料更新接口请求
 * • 用户信息展示页面数据绑定
 * • 用户列表查询结果封装
 *
 * 设计特点：
 * • 提供多参数构造方法便于对象创建
 * • 默认构造方法支持序列化需求
 * • 完整的Getter/Setter方法支持数据绑定
 */
package com.example.jpaspringboot.dto;

public class UserInfoDTO {
    private String name;
    private String email;
    private String birthdate;  // 出生日期使用字符串格式简化存储
    private String avatar;
    public UserInfoDTO(String name, String email, String birthdate) {
        this.name = name;
        this.email = email;
        this.birthdate = birthdate;
        this.avatar = null;
    }
    public UserInfoDTO() {
    }
    public UserInfoDTO(String name, String email, String birthdate, String avatar) {
        this.name = name;
        this.email = email;
        this.birthdate = birthdate;
        this.avatar = avatar;
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
    public String getAvatar() {
        return avatar;
    }

    /**
     * 设置
     * @param avatar
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String toString() {
        return "UserInfoDTO{name = " + name + ", email = " + email + ", birthdate = " + birthdate + ", avatar = " + avatar + "}";
    }
}
