package com.example.jpaspringboot.service;

import com.example.jpaspringboot.dto.UserInfoDTO;
import com.example.jpaspringboot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

public interface UserService {
//    List<User> selectAll();



    List<User> selectUsersInRange(int startId, int endId);

    @ResponseBody int addUser(@RequestParam String name, @RequestParam String password);

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

    @ResponseBody int addUser(@RequestParam String name, @RequestParam String password,
                              @RequestParam String email, @RequestParam String birthdate);

    @ResponseBody boolean updateUser(@RequestParam int id, @RequestParam String name, @RequestParam String password);


    User findByUsername(String username);

//    List<User> selectRandomUsersInRange();

    //    其实这些管理员操作我都不是很想加上去，改变用户的邮箱和生日有用吗？邮箱忘记了那用户就登不上去了，很少更改邮箱的业务要求吧？
    @ResponseBody boolean updateUser(@RequestParam int id, @RequestParam String name,
                                     @RequestParam String password,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) String birthdate);

    boolean updateCurrentUserInfo(String token, String name, String password, String email, String birthdate);

    @ResponseBody boolean deleteUserById(@RequestParam Long id);;

    @ResponseBody boolean deleteUserByIds(@RequestParam List<Long> ids);

//    List<User> findUsersManagedByAdmin(String token);

    Page<User> findUsersManagedByAdmin(String token, Pageable pageable);


    boolean verifyUser(String username, String email, String newpassword);

    boolean resetUserPassword(String username, String newPassword);

    UserInfoDTO getCurrentUserInfo(String token);
}

