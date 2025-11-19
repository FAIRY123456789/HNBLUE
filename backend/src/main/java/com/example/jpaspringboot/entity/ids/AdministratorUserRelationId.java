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

    // Constructors, getters, setters, hashCode and equals methods
}

