/**
 * 用户服务实现类
 *
 * 功能概述：
 * • 提供完整的用户管理功能，包括注册、认证、信息维护等
 * • 实现基于JWT令牌的身份验证和权限控制
 * • 支持管理员对关联用户的分页查询和搜索
 * • 集成邮箱验证和密码重置安全流程
 *
 * 核心业务：
 * • 用户注册与密码加密存储（SHA-256 + Salt）
 * • 用户信息修改与个性化设置
 * • 邮箱验证与密码重置流程
 * • 管理员-用户关系管理
 *
 * 安全特性：
 * • 密码加盐哈希存储防止彩虹表攻击
 * • JWT令牌验证用户身份
 * • 邮箱验证保障账户安全
 * • 细粒度权限控制
 *
 * 数据流程：
 * • 用户请求 → JWT令牌解析 → 权限验证 → 业务处理 → 响应返回
 * • 密码重置：邮箱验证 → 令牌生成 → 链接发送 → 密码更新
 */
package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.dto.UserInfoDTO;
import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.exception.UserAlreadyExistsException;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.AdministratorUserRelationRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.service.UserService;
import com.example.jpaspringboot.util.JwtUtils;
import com.example.jpaspringboot.util.PasswordPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdministratorUserRelationRepository administratorUserRelationRepository;

    @Autowired
    private EmailServiceImpl emailServiceImpl;

    /**
     * 查询指定ID范围内的用户列表
     */
    @Override
    public List<User> selectUsersInRange(int startId, int endId) {
        System.out.println("从数据库查询指定范围内的用户");
        return userRepository.findByIdBetween(startId, endId);
    }

    @Override
    public int addUser(String name, String password) {
        String normalizedName = normalizeUsername(name);
        if (normalizedName == null) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return addUser(normalizedName, password,
                normalizedName.toLowerCase(Locale.ROOT) + "@loadtest.invalid", "2000-01-01");
    }

    /**
     * 根据用户名查找用户
     */
    @Override
    public User findByUsername(String username) {
        return userRepository.findByName(username);
    }

    /**
     * 添加新用户（管理员操作）
     * 执行用户名重复性检查，密码加密处理
     */
    @ResponseBody
    @Override
    public int addUser(@RequestParam String name, @RequestParam String password,
                       @RequestParam String email, @RequestParam String birthdate) {
        String normalizedName = normalizeUsername(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedBirthdate = normalizeBirthdate(birthdate);
        PasswordPolicy.requireValid(password);
        User existingUser = userRepository.findByName(normalizedName);
        if (existingUser == null) {
            try {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                User newUser = new User(normalizedName, salt, passwordHash, normalizedEmail, normalizedBirthdate);
                newUser = userRepository.save(newUser);
                System.out.println("??????!");
                return newUser.getId();
            } catch (IllegalArgumentException | DataIntegrityViolationException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("??????: " + e.getClass().getSimpleName());
                throw new RuntimeException("??????!");
            }
        }
        throw new UserAlreadyExistsException("??????! ??????!");
    }

    @Override
    public boolean updateUser(int id, String name, String password) {
        return false;
    }

    /**
     * 更新用户信息（管理员操作）
     * 支持用户名、密码、邮箱、生日的单独或批量更新
     */
    @ResponseBody
    @Override
    public boolean updateUser(@RequestParam int id, @RequestParam String name,
                              @RequestParam String password,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String birthdate) {
        Optional<User> optionalUser = userRepository.findById((long) id);
        if (optionalUser.isEmpty()) {
            System.out.println("???ID? " + id + " ???");
            return false;
        }
        User existingUser = optionalUser.get();
        String normalizedName = normalizeUsername(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedBirthdate = normalizeBirthdate(birthdate);
        if (password != null && !password.isBlank()) {
            PasswordPolicy.requireValid(password);
        }
        String existingpasswordHash = (password == null || password.isBlank()) ? existingUser.getPasswordHash() : hashPassword(password, existingUser.getSalt());
        boolean isNameChanged = normalizedName != null && !normalizedName.equals(existingUser.getName());
        boolean isPasswordChanged = password != null && !password.isBlank() && !existingpasswordHash.equals(existingUser.getPasswordHash());
        boolean isEmailChanged = normalizedEmail != null && !normalizedEmail.equals(existingUser.getEmail());
        boolean isBirthdateChanged = normalizedBirthdate != null && !normalizedBirthdate.equals(existingUser.getBirthdate());
        try {
            if (isNameChanged) existingUser.setName(normalizedName);
            if (isPasswordChanged) {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                existingUser.setSalt(salt);
                existingUser.setPasswordHash(passwordHash);
            }
            if (isEmailChanged) existingUser.setEmail(normalizedEmail);
            if (isBirthdateChanged) existingUser.setBirthdate(normalizedBirthdate);
            if (isNameChanged || isPasswordChanged || isEmailChanged || isBirthdateChanged) {
                userRepository.save(existingUser);
                System.out.println("????????!");
                return true;
            }
            System.out.println("???????????");
            return false;
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            throw e;
        } catch (Exception e) {
            System.out.println("????????: " + e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean updateCurrentUserInfo(String token, String name, String password, String email, String birthdate) {
        String cleanToken = token == null ? "" : token.replace("Bearer ", "");
        String username = JwtUtils.extractUsername(cleanToken);
        User user = userRepository.findByName(username);
        if (user == null) {
            System.out.println("???????");
            return false;
        }
        boolean isChanged = false;
        String normalizedName = normalizeUsername(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedBirthdate = normalizeBirthdate(birthdate);
        if (normalizedName != null && !normalizedName.equals(user.getName())) {
            user.setName(normalizedName);
            isChanged = true;
        }
        if (password != null && !password.isEmpty()) {
            PasswordPolicy.requireValid(password);
            String hashed = hashPassword(password, user.getSalt());
            if (!hashed.equals(user.getPasswordHash())) {
                String newSalt = generateSalt();
                String newHash = hashPassword(password, newSalt);
                user.setSalt(newSalt);
                user.setPasswordHash(newHash);
                isChanged = true;
            }
        }
        if (normalizedEmail != null && !normalizedEmail.equals(user.getEmail())) {
            user.setEmail(normalizedEmail);
            isChanged = true;
        }
        if (normalizedBirthdate != null && !normalizedBirthdate.equals(user.getBirthdate())) {
            user.setBirthdate(normalizedBirthdate);
            isChanged = true;
        }
        if (isChanged) {
            userRepository.save(user);
            System.out.println("??????????");
            return true;
        }
        System.out.println("????????");
        return false;
    }

    public @ResponseBody int addUserSelf(@RequestParam String name, @RequestParam String password,
                                         @RequestParam String email, @RequestParam String birthdate) {
        String normalizedName = normalizeUsername(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedBirthdate = normalizeBirthdate(birthdate);
        PasswordPolicy.requireValid(password);
        User existingUser = userRepository.findByName(normalizedName);
        if (existingUser == null) {
            try {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                User newUser = new User(normalizedName, salt, passwordHash, normalizedEmail, normalizedBirthdate);
                newUser = userRepository.save(newUser);
                System.out.println("??????!");
                return newUser.getId();
            } catch (IllegalArgumentException | DataIntegrityViolationException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("??????: " + e.getClass().getSimpleName());
                throw new RuntimeException("??????!");
            }
        }
        throw new UserAlreadyExistsException("??????! ??????!");
    }

    @Override
    public boolean deleteUserById(Long id) {
        try {
            userRepository.deleteById(id);
            return true;
        } catch (EmptyResultDataAccessException e) {
            System.out.println("未找到ID为 " + id + " 的用户");
        }
        return false;
    }

    /**
     * 批量删除用户
     */
    @Override
    public boolean deleteUserByIds(List<Long> ids) {
        try {
            for (Long id : ids) {
                deleteUserById(id);
            }
            return true;
        } catch (EmptyResultDataAccessException e) {
            System.out.println("未找到ID列表为 " + ids + " 的用户");
        }
        return false;
    }

    /**
     * 分页查询管理员关联的用户列表
     */
    @Override
    public Page<User> findUsersManagedByAdmin(String token, Pageable pageable) {
        Admin admin = getAdminFromToken(token);
        if (isPrimaryAdmin(admin)) {
            return userRepository.findAll(pageable);
        }

        // Page relation IDs through the composite (admin_id, user_id) key first, then
        // batch-load the 20 user entities. A native query declared on the relation
        // repository cannot reliably materialize a different entity type (User).
        // Passing the controller's Sort.by("id") through would also append ORDER BY
        // r.id even though the relation table has no standalone id column.
        Pageable relationPageable = Pageable.ofSize(pageable.getPageSize())
                .withPage(pageable.getPageNumber());
        Page<Integer> userIdPage = administratorUserRelationRepository
                .findUserIdsByAdminId(admin.getId(), relationPageable);
        if (userIdPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, userIdPage.getTotalElements());
        }

        Map<Integer, User> usersById = userRepository.findByIdIn(userIdPage.getContent()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<User> orderedUsers = userIdPage.getContent().stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageImpl<>(orderedUsers, pageable, userIdPage.getTotalElements());
    }

    /**
     * 用户认证验证
     */
    public boolean authenticateUser(String name, String inputPassword) {
        User user = userRepository.findByName(name);
        if (user == null) {
            return false;
        }

        String expectedHash = hashPassword(inputPassword, user.getSalt());
        return expectedHash.equals(user.getPasswordHash());
    }

    /**
     * 用户注册
     */
    public boolean registerUser(String name, String password, String email, String birthdate) {
        addUserSelf(name, password, email, birthdate);
        return true;
    }

    /**
     * 生成并保存测试用户数据
     */
    public void generateAndSaveTestUsers() {
        String password = System.getenv("HNBLUE_TEST_USER_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("HNBLUE_TEST_USER_PASSWORD is required");
        }
        for (int i = 100002; i <= 100999; i++) {
            String username = "test_user" + i;
            addUser(username, password);
        }
    }

    /**
     * 生成随机盐值（16字节）
     */
    String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 密码哈希计算（SHA-256 + Salt）
     */
    String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String saltedPassword = salt + password;
            byte[] hashedBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码哈希计算错误", e);
        }
    }

    /**
     * 验证用户身份（用户名和邮箱匹配）
     * 用于密码重置前的身份验证
     */
    @Override
    public boolean verifyUser(String username, String email, String newpassword) {
        PasswordPolicy.requireValid(newpassword);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByName(normalizedUsername));
        if (userOpt.isPresent() && Objects.equals(normalizeEmail(userOpt.get().getEmail()), normalizedEmail)) {
            sendVerificationEmail(userOpt.get(), newpassword);
            return true;
        }
        return false;
    }

    private void sendVerificationEmail(User user, String newPassword) {
        String encryptedPassword = encryptNewPassword(newPassword);
        String token = generateVerificationToken(user);
        String verificationLink = "http://localhost:8088/api/verifyToken?token=" + token + "&newPassword=" + encryptedPassword;

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String mailContent = user.getName() + "，您好：\n\n" +
                "感谢您使用HNBLUE！\n" +
                "请点击如下链接，以完成您邮箱的绑定：\n" +
                verificationLink + "\n" +
                "(如果不能点击该链接地址，请复制并粘贴到浏览器的地址输入框)\n\n" +
                "HNBLUE\n" +
                currentDate;

        emailServiceImpl.send(user.getEmail(), "验证您的邮箱", mailContent);
    }

    /**
     * 加密新密码（Base64编码）
     */
    private String encryptNewPassword(String newPassword) {
        return Base64.getEncoder().encodeToString(newPassword.getBytes());
    }

    /**
     * 生成邮箱验证令牌（JWT）
     */
    private String generateVerificationToken(User user) {
        return JwtUtils.generateTokenWithClaims(user);
    }

    /**
     * 验证令牌有效性
     */
    public boolean verifyToken(String token) {
        try {
            Integer userId = JwtUtils.extractUserId(token);
            if (userId == null) {
                return false;
            }

            Optional<User> userOptional = userRepository.findById(userId.longValue());
            if (userOptional.isEmpty()) {
                return false;
            }
            User user = userOptional.get();

            return JwtUtils.validateToken(token, user);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 重置用户密码
     */
    @Override
    public boolean resetUserPassword(String username, String newPassword) {
        PasswordPolicy.requireValid(newPassword);
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByName(normalizeUsername(username)));
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);
            user.setSalt(newSalt);
            user.setPasswordHash(newHashedPassword);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public String getUsernameById(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId.longValue());
        if (userOptional.isEmpty()) {
            return "";
        }
        User user = userOptional.get();
        return user.getName();
    }

    /**
     * 获取当前用户信息（包含头像Base64编码）
     */
    @Override
    public UserInfoDTO getCurrentUserInfo(String token) {
        String username = JwtUtils.extractUsername(token);
        User user = userRepository.findByName(username);
        if (user == null) {
            throw new RuntimeException("当前用户不存在");
        }
        String avatarBase64 = user.getAvatar() == null ? null : Base64.getEncoder().encodeToString(user.getAvatar());
        String lastLoginAt = user.getLastLoginAt() == null ? null : user.getLastLoginAt().toString();
        return new UserInfoDTO(user.getId(), user.getName(), user.getEmail(), user.getBirthdate(), avatarBase64, "User", lastLoginAt);
    }

    /**
     * 根据用户名模糊查询用户
     */

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void markLastLogin(String username) {
        User user = userRepository.findByName(username);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    public List<User> findUsersByNameContaining(String name) {
        return userRepository.findByNameContaining(name);
    }

    /**
     * 更新用户头像
     */
    public byte[] updateAvatar(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("不能存储空文件");
        }

        byte[] fileBytes = file.getBytes();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用户未找到"));
        user.setAvatar(fileBytes);
        userRepository.save(user);

        return fileBytes;
    }

    /**
     * 根据令牌获取用户ID
     */
    public int getUserIDByToken(String token) {
        String username = JwtUtils.extractUsername(token);
        User user = userRepository.findByName(username);
        if(user != null){
            return user.getId();
        }else {
            return 0;
        }
    }

    /**
     * 管理员搜索关联用户（支持关键词过滤）
     */
        public Page<User> searchUsersByAdmin(String token, String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return findUsersManagedByAdmin(token, pageable);
        }

        Admin admin = getAdminFromToken(token);
        if (isPrimaryAdmin(admin)) {
            return userRepository.searchByKeyword(normalizedKeyword, pageable);
        }

        return administratorUserRelationRepository.searchUsersByAdminId(
                admin.getId(), normalizedKeyword, pageable);
    }

    public User findUserForAdmin(String token, Long userId) {
        User user = findById(userId);
        if (user == null) {
            return null;
        }
        Admin admin = getAdminFromToken(token);
        if (isPrimaryAdmin(admin) || canAdminAccessUser(admin, user)) {
            return user;
        }
        return null;
    }

    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    private Admin getAdminFromToken(String token) {
        String adminName = JwtUtils.getUsernameFromToken(token);
        Admin admin = adminRepository.findByName(adminName);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        return admin;
    }

    private boolean isPrimaryAdmin(Admin admin) {
        return admin != null && (Integer.valueOf(100000).equals(admin.getId()) || "Admin_100000".equals(admin.getName()));
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String value = username.trim();
        return value.isEmpty() ? null : value;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String value = email.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    private String normalizeBirthdate(String birthdate) {
        if (birthdate == null) {
            return null;
        }
        String value = birthdate.trim().replace('/', '-');
        try {
            return LocalDate.parse(value).toString();
        } catch (Exception ex) {
            return value;
        }
    }

    private boolean canAdminAccessUser(Admin admin, User user) {
        if (admin == null || user == null) {
            return false;
        }
        return administratorUserRelationRepository.existsRelation(admin.getId(), user.getId());
    }
}
