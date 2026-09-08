/**
 * UserServiceImplTest
 *
 * 本测试类对 UserServiceImpl 进行集成测试，主要验证以下场景：
 * 1. 删除单个用户（deleteUserById_ExistingUser_Success）
 * 2. 删除多个用户（deleteUserByIds_MultipleUsers_Success）
 * 3. 根据用户名查询用户（findByUsername_ExistingUser_Found）
 * 4. 验证用户登录（authenticateUser_ValidCredentials_ReturnsTrue）
 * 5. 注册新用户（registerUser_NewUser_ReturnsTrue）
 *
 * 使用 @SpringBootTest 进行集成测试，结合 Mockito 进行依赖模拟和验证。
 */
package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.AdministratorUserRelationRepository;
import com.example.jpaspringboot.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdministratorUserRelationRepository administratorUserRelationRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试方法：deleteUserById_ExistingUser_Success
     *
     * 测试场景：验证删除单个存在用户的操作是否成功。
     * 步骤：
     * 1. 模拟 deleteById 方法正常执行。
     * 2. 调用 deleteUserById(1L) 方法。
     * 3. 验证返回值为 true，且 deleteById 方法调用了一次。
     */
    @Test
    public void deleteUserById_ExistingUser_Success() {
        doNothing().when(userRepository).deleteById(1L);

        boolean result = userServiceImpl.deleteUserById(1L);

        assertTrue(result);
        verify(userRepository, times(1)).deleteById(1L);
    }

    /**
     * 测试方法：deleteUserByIds_MultipleUsers_Success
     *
     * 测试场景：验证删除多个用户的操作是否成功。
     * 步骤：
     * 1. 模拟 deleteById 方法对任意 ID 正常执行。
     * 2. 调用 deleteUserByIds 方法传入多个 ID。
     * 3. 验证返回值为 true，且 deleteById 方法调用次数与 ID 数量一致。
     */
    @Test
    public void deleteUserByIds_MultipleUsers_Success() {
        doNothing().when(userRepository).deleteById(anyLong());

        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        boolean result = userServiceImpl.deleteUserByIds(ids);

        assertTrue(result);
        verify(userRepository, times(ids.size())).deleteById(anyLong());
    }

    /**
     * 测试方法：findByUsername_ExistingUser_Found
     *
     * 测试场景：验证通过用户名查找用户是否成功。
     * 步骤：
     * 1. 模拟 userRepository 返回指定用户。
     * 2. 调用 findByUsername 方法。
     * 3. 验证返回用户对象不为空，且用户名正确。
     */
    @Test
    public void findByUsername_ExistingUser_Found() {
        User user = new User();
        user.setId(1);
        user.setName("existingUser");

        when(userRepository.findByName("existingUser")).thenReturn(user);

        User foundUser = userServiceImpl.findByUsername("existingUser");

        assertNotNull(foundUser);
        assertEquals("existingUser", foundUser.getName());
        verify(userRepository, times(1)).findByName("existingUser");
    }

    /**
     * 测试方法：authenticateUser_ValidCredentials_ReturnsTrue
     *
     * 测试场景：验证使用有效用户名和密码登录是否成功。
     * 步骤：
     * 1. 模拟用户信息和密码哈希计算。
     * 2. 调用 authenticateUser 方法。
     * 3. 验证返回值为 true。
     */
    @Test
    public void authenticateUser_ValidCredentials_ReturnsTrue() {
        String username = "username";
        String password = "password";
        String salt = userServiceImpl.generateSalt();
        User user = new User();
        user.setName(username);
        user.setSalt(salt);
        user.setPasswordHash(userServiceImpl.hashPassword(password, salt));

        when(userRepository.findByName(username)).thenReturn(user);

        boolean result = userServiceImpl.authenticateUser(username, password);

        assertTrue(result);
    }

    /**
     * 测试方法：registerUser_NewUser_ReturnsTrue
     *
     * 测试场景：验证注册新用户操作是否成功。
     * 步骤：
     * 1. 模拟 userRepository 返回 null 表示用户名未占用。
     * 2. 调用 registerUser 方法。
     * 3. 验证返回值为 true，且保存操作调用一次。
     */
    @Test
    public void registerUser_NewUser_ReturnsTrue() {
        User mockUser = new User();
        mockUser.setId(1);

        when(userRepository.findByName("newUser")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        boolean result = userServiceImpl.registerUser("newUser", "Password123", "email", "birthdate");

        assertTrue(result);
        verify(userRepository, times(1)).findByName("newUser");
        verify(userRepository, times(1)).save(any(User.class));
    }
}
