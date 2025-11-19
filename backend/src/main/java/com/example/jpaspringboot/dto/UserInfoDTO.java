package com.example.jpaspringboot.dto;

public class UserInfoDTO {
    private String name;
    private String email;
    private String birthdate;  // Assuming birthdate is stored as String for simplicity
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
