package com.example.jpaspringboot.entity;

import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
public class AdministratorUserRelation {
    @EmbeddedId
    private AdministratorUserRelationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("adminId")
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    public AdministratorUserRelation() {
    }

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


    // Constructors, getters, setters
}