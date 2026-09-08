package com.example.jpaspringboot.dto;

public class UserInfoDTO {
    private Integer id;
    private String name;
    private String username;
    private String email;
    private String birthdate;
    private String avatar;
    private String role;
    private String lastLoginAt;

    public UserInfoDTO() {
    }

    public UserInfoDTO(String name, String email, String birthdate) {
        this(null, name, email, birthdate, null, "User", null);
    }

    public UserInfoDTO(String name, String email, String birthdate, String avatar) {
        this(null, name, email, birthdate, avatar, "User", null);
    }

    public UserInfoDTO(Integer id, String name, String email, String birthdate, String avatar, String role, String lastLoginAt) {
        this.id = id;
        this.name = name;
        this.username = name;
        this.email = email;
        this.birthdate = birthdate;
        this.avatar = avatar;
        this.role = role;
        this.lastLoginAt = lastLoginAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; this.username = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBirthdate() { return birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    @Override
    public String toString() {
        return "UserInfoDTO{id=" + id + ", name=" + name + ", email=" + email + ", birthdate=" + birthdate + ", role=" + role + "}";
    }
}
