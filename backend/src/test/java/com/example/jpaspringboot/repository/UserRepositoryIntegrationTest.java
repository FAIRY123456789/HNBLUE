/**
 * UserRepositoryIntegrationTest
 *
 * 本测试类对 UserRepository 进行集成测试，主要验证以下场景：
 * 1. 根据用户名查询用户（whenFindByName_thenReturnUser）
 * 2. 根据 ID 范围查询用户列表（whenFindByIdBetween_thenReturnUsers）
 * 3. 保存用户并验证持久化（whenSave_thenReturnSavedUser）
 *
 * 使用 @DataJpaTest 进行 JPA 层测试，结合 TestEntityManager 进行操作校验。
 */
package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
public class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    /**
     * 测试方法：whenFindByName_thenReturnUser
     *
     * 测试场景：验证通过用户名查询用户是否正确。
     * 步骤：
     * 1. 创建并保存一个用户实体。
     * 2. 通过名称查询用户。
     * 3. 验证返回的用户对象不为空且名称正确。
     */
    @Test
    public void whenFindByName_thenReturnUser() {
        User user = new User("TestUser", "salt", "hashpwd", "testuser@example.com", "2000-01-01");
        entityManager.persist(user);
        entityManager.flush();

        User found = userRepository.findByName(user.getName());

        assertNotNull(found);
        assertEquals(found.getName(), user.getName());
        assertEquals(found.getEmail(), "testuser@example.com");
        assertEquals(found.getBirthdate(), "2000-01-01");
    }

    /**
     * 测试方法：whenFindByIdBetween_thenReturnUsers
     *
     * 测试场景：验证通过 ID 范围查询多个用户是否正确。
     * 步骤：
     * 1. 创建并保存两个用户实体。
     * 2. 通过 ID 范围查询用户列表。
     * 3. 验证返回的列表不为空，且包含预期数量的用户。
     */
    @Test
    public void whenFindByIdBetween_thenReturnUsers() {
        User user1 = new User("TestUser1", "salt", "hashpwd", "user1@example.com", "2001-02-02");
        User user2 = new User("TestUser2", "salt", "hashpwd", "user2@example.com", "2002-03-03");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        List<User> users = userRepository.findByIdBetween(1, 10); // 假设 ID 范围

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());
        assertEquals(users.get(0).getEmail(), "user1@example.com");
        assertEquals(users.get(1).getBirthdate(), "2002-03-03");
    }

    /**
     * 测试方法：whenSave_thenReturnSavedUser
     *
     * 测试场景：验证保存用户实体并检查是否持久化。
     * 步骤：
     * 1. 创建用户对象并保存到数据库。
     * 2. 验证保存操作成功，返回的对象非空且字段匹配。
     */
    @Test
    public void whenSave_thenReturnSavedUser() {
        User user = new User("TestUser4", "salt", "hashpwd", "testuser4@example.com", "1999-12-31");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser);
        assertEquals(savedUser.getName(), "TestUser4");
        assertEquals(savedUser.getEmail(), "testuser4@example.com");
        assertEquals(savedUser.getBirthdate(), "1999-12-31");
    }
}
