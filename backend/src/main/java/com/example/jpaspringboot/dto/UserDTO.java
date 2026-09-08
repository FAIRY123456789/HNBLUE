/**
 * 用户数据传输对象 (Data Transfer Object)
 *
 * 功能概述：
 * • 在表示层与服务层之间传输用户数据
 * • 封装用户核心属性，避免暴露实体类细节
 * • 支持用户注册、登录、信息更新等业务场景
 *
 * 数据字段说明：
 * • id - 用户唯一标识
 * • name - 用户名称
 * • password - 用户密码（需加密传输）
 * • email - 用户邮箱地址
 * • birthdate - 用户出生日期
 *
 * 设计规范：
 * • 实现简单的POJO结构，便于序列化
 * • 提供完整的构造方法支持不同业务场景
 * • 遵循JavaBean规范，便于框架自动绑定
 *
 * 使用场景：
 * • REST API请求参数接收
 * • 服务层方法参数传递
 * • 控制器返回数据封装
 */
package com.example.jpaspringboot.dto;

public class UserDTO {

    private Integer id;
    private String name;
    private String password;
    private String email;
    private String birthdate;


    public UserDTO() {
    }

    public UserDTO(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public UserDTO(Integer id, String name, String password, String email, String birthdate) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.email = email;
        this.birthdate = birthdate;
    }

    /**
     * 获取
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(int id) {
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
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserDTO{id=" + id + ", name='" + name + "', password=[PROTECTED], email='" + email + "', birthdate='" + birthdate + "'}";
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
}

