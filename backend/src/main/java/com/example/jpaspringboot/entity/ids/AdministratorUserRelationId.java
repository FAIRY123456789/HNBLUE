/**
 * 管理员-用户关系复合主键类
 *
 * 功能概述：
 * • 定义管理员与用户多对多关系的复合主键
 * • 作为JPA实体类的嵌入主键组件使用
 * • 支持序列化用于网络传输和缓存存储
 *
 * 设计说明：
 * • 使用@Embeddable标记为可嵌入主键类
 * • 实现Serializable接口保证序列化能力
 * • 包含管理员ID和用户ID两个关联字段
 *
 * 使用场景：
 * • 管理员用户关系表的联合主键
 * • JPA多对多关联关系的标识符
 * • 权限管理系统的用户分配记录
 *
 * 技术要求：
 * • 需要无参构造器供JPA实例化
 * • 字段不可变以保证主键稳定性
 */
package com.example.jpaspringboot.entity.ids;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Embeddable
public class AdministratorUserRelationId implements Serializable {
    private Integer adminId;
    private Integer userId;

    public AdministratorUserRelationId() {
    }

    public AdministratorUserRelationId(Integer adminId, Integer userId) {
        this.adminId = adminId;
        this.userId = userId;
    }

    /**
     * 获取
     * @return adminId
     */
    public Integer getAdminId() {
        return adminId;
    }

    /**
     * 设置
     * @param adminId
     */
    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    /**
     * 获取
     * @return userId
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * 设置
     * @param userId
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String toString() {
        return "AdministratorUserRelationId{adminId = " + adminId + ", userId = " + userId + "}";
    }

}

