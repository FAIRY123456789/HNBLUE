package com.example.jpaspringboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;


@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity // This tells Hibernate to make a table out of this class
public class User {
    /**
     * ，如果你使用JPA或其他ORM工具，并配置了自动递增的ID，那么在构造函数中通常不需要手动提供ID。
     * 由于ID是自动生成的，所以在创建新的User实例时，不需要手动设置它。
     * */

    /**
     * @GeneratedValue(strategy=GenerationType.AUTO)
     * GenerationType.AUTO是默认的生成策略，JPA提供程序会根据数据库选择最佳策略。这可能是IDENTITY，SEQUENCE或TABLE，取决于所使用的数据库和提供者。
     *
     * 如果你知道所使用的数据库支持并且通常使用自增ID，那么明确选择GenerationType.IDENTITY是一个好主意。
     * ]这样，你的代码的意图会更清晰，并且你可以确保JPA提供者会使用你期望的策略。
     * */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //数据库会在插入新记录时自动为你生成一个新的ID。这种策略在大多数RDBMS（例如MySQL）中都很常见。
    private Integer id;

    @Column(unique = true)
    private String name;

    private String salt;//在实践中，通常不会为盐设置数据库层面的唯一性约束。
    private String passwordHash; // 存储哈希值，而不是实际密码

    @Column(unique = true, nullable = false)
    private String email; // 新增电子邮箱字段，不允许为空，且唯一

    @Column(nullable = false)
    private String birthdate; // 新增出生日期字段，不允许为空

    @Lob
    @Column(name = "avatar",columnDefinition = "LONGBLOB")
    private byte[] avatar;  // 用于存储图片的二进制数据

    public User() {
    }


    /***
     * 如果你使用JPA或其他ORM工具，并配置了自动递增的ID，那么在构造函数中通常不需要手动提供ID。
     * 由于ID是自动生成的，所以在创建新的User实例时，不需要手动设置它。
     *
     * 卧槽，我忘记改User了，然后发现居然新增的属性都被自动映射到数据库中了，这么牛逼？？
     * @param name
     * @param salt
     * @param passwordHash
     */
    public User(String name, String salt, String passwordHash) {
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public User(int id,String name, String salt, String passwordHash) {
        this.id = id;
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public User(String name, String salt, String passwordHash, String email, String birthdate) {
        this.name = name;
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.email = email;
        this.birthdate = birthdate;
    }


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