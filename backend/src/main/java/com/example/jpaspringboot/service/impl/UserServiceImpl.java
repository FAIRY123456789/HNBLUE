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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

//    @Cacheable(value = "allUsers", sync = true) 设置互斥锁，有效解决缓存击穿问题
//    @Cacheable(value = "allUsers")

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdministratorUserRelationRepository administratorUserRelationRepository;

    @Autowired
    private EmailServiceImpl emailServiceImpl;


//    @Override
//    public List<User> selectAll() {
//        System.out.println("Fetching from database");
//        return userRepository.findAll();
//    }

    @Override
    public List<User> selectUsersInRange(int startId, int endId) {
        System.out.println("Fetching users in range from database");
        return userRepository.findByIdBetween(startId, endId);
    }

    @Override
    public int addUser(String name, String password) {
        return 0;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByName(username);
    }

//    @Override
//    public List<User> selectRandomUsersInRange() {
//        Random random = new Random();
//        int randomId1 = random.nextInt(2000) + 1;// 随机生成1到10000范围内的ID
//        int randomId2 = random.nextInt(2001,2500) + 1;
//        return userRepository.findByIdBetween(randomId1, randomId2);
//    }


    /**
     * 异常处理：你可以选择返回null，但在实际应用中，最好是抛出一个自定义的异常，然后在控制器层捕获这个异常并返回适当的HTTP状态码和消息。
     *
     * 返回值：你的方法返回了User对象，包括所有用户的信息。在实际的应用中，可能不希望返回密码的盐值和哈希值。你可以创建一个DTO（数据传输对象）只包含用户的公开信息返回给前端。
     *
     * 用户存在的判断：如果用户名已存在，返回null可能会让前端难以区分是添加失败还是发生了其他错误。你可以抛出一个异常或者返回一个特定的错误对象或消息。
     * @param name
     * @param password
     * @return
     */
//    @Override
//    public @ResponseBody User addUser(@RequestParam String name, @RequestParam String password) {
//        User user = userRepository.findByName(name);
//        if (user==null){
//            try {
//                String salt = generateSalt();
//                String passwordHash = hashPassword(password, salt);
//                //使用这些方法，每当你存储一个新用户的密码或验证现有用户的密码时，你都可以确保你在处理的是密码的盐和哈希值，而不是密码本身。
//                user = new User(name, salt, passwordHash);
//                userRepository.save(user);
//                System.out.println("User added!");
//                return user;
//            } catch (Exception e) {
//                System.out.println("Error adding user: " + e.getMessage());
//                return null;
//            }
//        }
//        System.out.println("Username is duplicated!");
//        return null;
//    }
    @ResponseBody
    @Override
    public int addUser(@RequestParam String name, @RequestParam String password,
                       @RequestParam String email, @RequestParam String birthdate) {
        User existingUser = userRepository.findByName(name);
        if (existingUser == null) {
            try {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                User newUser = new User(name, salt, passwordHash, email, birthdate);
                newUser = userRepository.save(newUser);
                System.out.println("用户添加成功!");

                return newUser.getId(); // 仅返回新用户的ID
            } catch (Exception e) {
                System.out.println("添加用户出错: " + e.getMessage());
                throw new RuntimeException("Fail to add user!"); // 选择抛出一个更具体的异常
            }
        } else {
            throw new UserAlreadyExistsException("Fail to add user! Username already exists!"); // 抛出自定义异常
        }
    }

    @Override
    public boolean updateUser(int id, String name, String password) {
        return false;
    }

    //    其实这些管理员操作我都不是很想加上去，改变用户的邮箱和生日有用吗？邮箱忘记了那用户就登不上去了，很少更改邮箱的业务要求吧？
@ResponseBody
@Override
public boolean updateUser(@RequestParam int id, @RequestParam String name,
                          @RequestParam String password,
                          @RequestParam(required = false) String email,
                          @RequestParam(required = false) String birthdate) {
    // 首先查找现有用户
    Optional<User> optionalUser = userRepository.findById((long) id);
    if (optionalUser.isPresent()) {
        User existingUser = optionalUser.get();
        String existingpasswordHash = hashPassword(password, existingUser.getSalt());

        boolean isNameChanged = !name.equals(existingUser.getName());
        boolean isPasswordChanged = !existingpasswordHash.equals(existingUser.getPasswordHash());

        boolean isEmailChanged = email != null && !email.equals(existingUser.getEmail());
        boolean isBirthdateChanged = birthdate != null && !birthdate.equals(existingUser.getBirthdate());

        try {
            if (isNameChanged) {
                existingUser.setName(name);
            }

            if (isPasswordChanged) {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
                existingUser.setSalt(salt);
                existingUser.setPasswordHash(passwordHash);
            }

            if (isEmailChanged) {
                existingUser.setEmail(email);
            }

            if (isBirthdateChanged) {
                existingUser.setBirthdate(birthdate);
            }

            if (isNameChanged || isPasswordChanged || isEmailChanged || isBirthdateChanged) {
                userRepository.save(existingUser);
                System.out.println("User Updated!");
                return true;
            } else {
                System.out.println("No changes detected to update.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Error updating user: " + e.getMessage());
            return false;
        }
    } else {
        System.out.println("User not found with ID: " + id);
        return false;
    }
}

    @Override
    public boolean updateCurrentUserInfo(String token, String name, String password, String email, String birthdate) {
        String username = JwtUtils.extractUsername(token);
        User user = userRepository.findByName(username);
        if (user == null) {
            System.out.println("当前用户未找到");
            return false;
        }

        boolean isChanged = false;

        // 修改用户名
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            isChanged = true;
        }

        // 修改密码（判断密码是否变化）
        if (password != null && !password.isEmpty()) {
            String hashed = hashPassword(password, user.getSalt());
            if (!hashed.equals(user.getPasswordHash())) {
                String newSalt = generateSalt();
                String newHash = hashPassword(password, newSalt);
                user.setSalt(newSalt);
                user.setPasswordHash(newHash);
                isChanged = true;
            }
        }

        // 修改邮箱
        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            isChanged = true;
        }

        // 修改生日
        if (birthdate != null && !birthdate.equals(user.getBirthdate())) {
            user.setBirthdate(birthdate);
            isChanged = true;
        }

        if (isChanged) {
            userRepository.save(user);
            System.out.println("当前用户信息已更新！");
            return true;
        } else {
            System.out.println("未检测到任何变更");
            return false;
        }
    }



    public @ResponseBody int addUserSelf(@RequestParam String name, @RequestParam String password, @RequestParam String email,@RequestParam String birthdate) {
        User existingUser = userRepository.findByName(name);
        if (existingUser == null) {
            try {
                String salt = generateSalt();
                String passwordHash = hashPassword(password, salt);
//                添加用户的时候就不那么麻烦了，不是重点内容
                User newUser = new User(name, salt, passwordHash, email, birthdate);
                newUser = userRepository.save(newUser);
                System.out.println("用户添加成功!");

                return newUser.getId(); // 仅返回新用户的ID
            } catch (Exception e) {
                System.out.println("添加用户出错: " + e.getMessage());
                throw new RuntimeException("Fail to add user!"); // 选择抛出一个更具体的异常
            }
        } else {
            throw new UserAlreadyExistsException("Fail to add user! Username already exists!"); // 抛出自定义异常
        }
    }

    @Override
    public boolean deleteUserById(Long id) {
        try {
            userRepository.deleteById(id);
            return true;
            //Empty...异常是由Spring Data JPA或底层持久化框架定义的，并不是自定义的
        } catch (EmptyResultDataAccessException e) {
            // 适当地处理异常，例如记录日志，抛出自定义异常等。
            System.out.println("User not found with id: " + id);
        }
        return false;
    }

    @Override
    public boolean deleteUserByIds(List<Long> ids) {

        try {
            for (Long id : ids) {
                deleteUserById(id);
            }
            return true;
        } catch (EmptyResultDataAccessException e) {
            // 适当地处理异常，例如记录日志，抛出自定义异常等。
            System.out.println("User not found with ids: " + ids);
        }
        return false;
    }

//    @Override
//    public List<User> findUsersManagedByAdmin(String token) {
//        String adminName = JwtUtils.getUsernameFromToken(token);
//        try {
//            Admin admin = adminRepository.findByName(adminName);
//
//            List<AdministratorUserRelation> relations = administratorUserRelationRepository.findByAdmin(admin);
//
//            return relations.stream()
//                    .map(AdministratorUserRelation::getUser)
//                    .collect(Collectors.toList());
//        }catch (RuntimeException e){
//            throw new RuntimeException("Admin not found with username: " + adminName);
//        }
//    }
    @Override
    public Page<User> findUsersManagedByAdmin(String token, Pageable pageable) {
        String adminName = JwtUtils.getUsernameFromToken(token);
        try {
            Admin admin = adminRepository.findByName(adminName);

            // 获取管理员关联的所有用户关系，并转换为用户ID列表
            List<Integer> userIds = administratorUserRelationRepository.findByAdmin(admin)
                    .stream()
                    .map(relation -> relation.getUser().getId())
                    .collect(Collectors.toList());

            // 使用用户ID列表从用户存储库中获取分页的用户列表
            if (!userIds.isEmpty()) {
                return userRepository.findAllById(userIds, pageable);
            } else {
                return new PageImpl<>(new ArrayList<>());
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Admin not found with username: " + adminName);
        }
    }


    public boolean authenticateUser(String name, String inputPassword) {
        User user = userRepository.findByName(name);
        if (user == null) {
            return false;
        }

        String expectedHash = hashPassword(inputPassword, user.getSalt());
        return expectedHash.equals(user.getPasswordHash());
    }

    public boolean registerUser(String name, String password, String email, String birthdate) {
        addUserSelf(name,password,email,birthdate);
        return true;
        //直接返回true就行了，因为add函数用户存在时直接报错，管它返回什么值呢
    }

    /**
     * Generate and save 100,000 test users to the database.
     * 为了防止电脑死机，我先从1k开始
     */
    public void generateAndSaveTestUsers() {
        for (int i = 100002; i <= 100999; i++) {
            String username = "test_user" + i;
            // 这是一个测试密码，实际应用中请勿使用过于简单的密码。
            String password = "123456";
            addUser(username, password);
        }
    }


    /***
     * 使用SecureRandom来生成随机盐。盐的长度通常为16字节，但可以根据需要调整。
     * @return
     */
    String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
        //使用Base64编码盐是因为我们在数据库中存储的是字符串。
        // 可以选择存储原始字节或使用其他编码方法，但Base64是常用且方便的选择。
    }


    /***
     * 使用MessageDigest类和SHA-256算法进行哈希。将盐附加到密码前面，然后计算其哈希值。
     * @param password
     * @param salt
     * @return
     */
    String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // 添加盐到密码前并计算哈希
            String saltedPassword = salt + password;
            byte[] hashedBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }


    /**
     * 验证提供的用户名和邮箱是否匹配现有用户。
     * 因为我设置的盐+哈希算法，不能直接修改密码，需要一口气将盐、（新密码形成的）哈希值一起修改，而且修改之前必须要经过邮箱验证。
     *
     * @param username 要验证的用户名
     * @param email 要验证的邮箱
     * @return 如果用户名和邮箱匹配现有用户，返回 true，否则返回 false
     */
    @Override
    public boolean verifyUser(String username, String email, String newpassword) {
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByName(username));
        if (userOpt.isPresent() && userOpt.get().getEmail().equals(email)) {
//            在用户验证通过后发送一封包含验证链接的邮件，实现基本的邮箱验证功能
            sendVerificationEmail(userOpt.get(),newpassword);
            return true;
        }
        return false;
    }

    /**
     *    实现发送验证邮件的方法
     */
//    private void sendVerificationEmail(User user) {
////        verificationLink变量保存了最终的URL，用户将通过点击这个URL来进行下一步操作。
///**这里的 /verify 是你应用中处理验证请求的端点。当你的前端或其他客户端收到这个链接并访问它时，它会向你的后端应用发起一个请求
//后端应用需要有一个相应的处理函数来验证令牌，并执行必要的操作（比如允许用户重置密码）。*/
//        String verificationLink = "http://localhost:8088/verifyToken?token=" + generateVerificationToken(user);//原先是userId来验证的
//
////        mailContent变量定义了邮件的正文内容，这里的内容提示用户点击下方的链接以完成密码重置过程。
//        String mailContent = "请点击以下链接以完成密码重置过程：" + verificationLink;
//
////        使用send方法将构造好的邮件发送到用户的邮箱。这个方法通常有三个参数：收件人邮箱地址、邮件主题和邮件内容。
//        emailServiceImpl.send(user.getEmail(), "验证您的邮箱", mailContent);
//    }

    private void sendVerificationEmail(User user, String newPassword) {
        // 加密新密码
        String encryptedPassword = encryptNewPassword(newPassword);
        // 生成验证token
        String token = generateVerificationToken(user);
        // 构建链接，包含token和加密后的新密码。第一次测试发现自己忘记把api写上去了
        String verificationLink = "http://localhost:8088/api/verifyToken?token=" + token + "&newPassword=" + encryptedPassword;

        // 获取当前日期
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // 格式化邮件内容，使其符合提供的模板
        String mailContent = user.getName() + "，您好：\n\n" +
                "感谢您使用HNBLUE！\n" +
                "请点击如下链接，以完成您邮箱的绑定：\n" +
                verificationLink + "\n" +
                "(如果不能点击该链接地址，请复制并粘贴到浏览器的地址输入框)\n\n" +
                "HNBLUE\n" +
                currentDate; // 使用当前日期替换原来的文本

        // 发送邮件
        emailServiceImpl.send(user.getEmail(), "验证您的邮箱", mailContent);
    }

    private String encryptNewPassword(String newPassword) {
        // 使用合适的加密方法
        return Base64.getEncoder().encodeToString(newPassword.getBytes());
    }


    /**
     * generateVerificationToken(user)生成一个包含用户ID的安全令牌（token）。
     * 这个令牌通常使用JWT（JSON Web Tokens）或其他安全机制生成，确保在链接被点击时可以安全地识别和验证用户。
     */
    private String generateVerificationToken(User user) {
        // 根据用户 ID 生成 JWT 令牌
        // 在 generateTokenWithClaims 方法中，我使用了一个 Map 来传递需要添加到 JWT 中的声明，即用户 ID。
        return JwtUtils.generateTokenWithClaims(user);
    }

    /**
     * 验证 token 的有效性并尝试重置密码
     *
     * @param token 用于验证的 JWT token
     * @return 验证结果，如果验证通过允许重置密码，返回 true，否则返回 false
     */
    public boolean verifyToken(String token) {
        try {
            // 从 token 中提取用户 ID
            Integer userId = JwtUtils.extractUserId(token);
            if (userId == null) {
                return false;
            }

            // 查找对应的用户
            Optional<User> userOptional = userRepository.findById(userId.longValue());
            if (userOptional.isEmpty()) {
                return false;
            }
            User user = userOptional.get();

            // 验证 token 是否有效
            return JwtUtils.validateToken(token, user);
        } catch (Exception e) {
            // 处理异常，例如记录日志
            return false;
        }
    }

    /**
     * 为给定用户名重置密码。
     *
     * @param username 需要重置密码的用户名
     * @param newPassword 新设的密码
     * @return 如果密码成功重置，返回 true，否则返回 false
     */
    @Override
    public boolean resetUserPassword(String username, String newPassword) {
//        既然邮箱验证已经通过，那其实说明肯定是有已存在的用户了，现在要做的就是把ta找出来然后save更新一下
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByName(username));
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String newSalt = generateSalt();
            String newHashedPassword = hashPassword(newPassword, newSalt);
            user.setSalt(newSalt);
            user.setPasswordHash(newHashedPassword);
//            只要id不变，那么save就是修改
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public String getUsernameById(Integer userId) {
        // 查找对应的用户
        Optional<User> userOptional = userRepository.findById(userId.longValue());
        if (userOptional.isEmpty()) {
            return "";
        }
        User user = userOptional.get();
        return user.getName();
    }

    @Override
    public UserInfoDTO getCurrentUserInfo(String token) {
        String username = JwtUtils.extractUsername(token);
        User user = userRepository.findByName(username);
        if(user.getAvatar() == null){
            return new UserInfoDTO(user.getName(), user.getEmail(), user.getBirthdate());
        }else{
            String avatarBase64 = Base64.getEncoder().encodeToString(user.getAvatar());
            return new UserInfoDTO(user.getName(), user.getEmail(), user.getBirthdate(), avatarBase64);
        }
    }

    public List<User> findUsersByNameContaining(String name) {
        return userRepository.findByNameContaining(name);
    }


    public byte[] updateAvatar(Long userId, MultipartFile file) throws IOException {
        // 检查文件不为空
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot store empty file");
        }

        // 读取文件为字节数组
        byte[] fileBytes = file.getBytes();

        // 找到用户并更新头像
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatar(fileBytes);
        userRepository.save(user);

        return fileBytes;
    }

    public int getUserIDByToken(String token) {
        String username = JwtUtils.extractUsername(token);
        User user = userRepository.findByName(username);
        if(user != null){
            return user.getId();
        }else {
            return 0;
        }
    }

    public Page<User> searchUsersByAdmin(String token, String keyword, Pageable pageable) {
        String adminName = JwtUtils.getUsernameFromToken(token);
        Admin admin = adminRepository.findByName(adminName);

        List<Integer> userIds = administratorUserRelationRepository.findByAdmin(admin)
                .stream().map(relation -> relation.getUser().getId())
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new PageImpl<>(new ArrayList<>());
        }

        return userRepository.searchByNameInIds(keyword, userIds, pageable);
    }

}

