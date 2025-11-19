package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import com.example.jpaspringboot.repository.AdminRepository;
import com.example.jpaspringboot.repository.AdministratorUserRelationRepository;
import com.example.jpaspringboot.repository.UserRepository;
import com.example.jpaspringboot.service.AdministratorUserRelationService;
import com.example.jpaspringboot.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdministratorUserRelationServiceImpl implements AdministratorUserRelationService {

    @Autowired
    private AdministratorUserRelationRepository relationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void deleteRelationForCurrentUser(Integer userId, String jwtToken) {
        // 首先获得当前用户（管理员）的名字
        String name = JwtUtils.getUsernameFromToken(jwtToken);

        // 然后根据管理员名字获取管理员实体
        Admin admin = null;

        //然后根据管理员名字获取管理员Id
        try {
            // 尝试根据管理员名字获取管理员实体
            admin = adminRepository.findByName(name);
            if (admin == null) {
                throw new RuntimeException("Admin not found with username " + name);
            }
        } catch (Exception e) {
            // 异常处理逻辑，例如记录日志等
            throw new RuntimeException("An error occurred while retrieving admin with username: " + name, e);
        }
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        // 创建关系ID
        AdministratorUserRelationId id = new AdministratorUserRelationId(admin.getId(), userId);

        // 删除管理关系
        relationRepository.deleteById(id);
    }


    @Override
    public AdministratorUserRelation createRelationForCurrentUser(Integer userId, String jwtToken) {

        //首先获得当前用户（管理员）的名字
        String name = JwtUtils.getUsernameFromToken(jwtToken);

        Admin admin = null;

        //然后根据管理员名字获取管理员Id
        try {
            // 尝试根据管理员名字获取管理员实体
            admin = adminRepository.findByName(name);
            if (admin == null) {
                throw new RuntimeException("Admin not found with username " + name);
            }
        } catch (Exception e) {
            // 异常处理逻辑，例如记录日志等
            throw new RuntimeException("An error occurred while retrieving admin with username: " + name, e);
        }
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new RuntimeException("User not found with id " + userId));

        //利用管理员id和用户id创建管理关系表
        AdministratorUserRelation relation = new AdministratorUserRelation();
        relation.setId(new AdministratorUserRelationId(admin.getId(), userId));

        //设置后，relation本质上存储的是adminId和userId，并把它们作为复合主键
        relation.setAdmin(admin);
        relation.setUser(user);

        return relationRepository.save(relation);
    }

    @Override
    public List<User> findUsersByAdmin(Admin admin) {
        List<AdministratorUserRelation> relations=relationRepository.findByAdmin(admin);
        List<User> users = new ArrayList<>();
        for (AdministratorUserRelation relation : relations){
            users.add(relation.getUser());
        }
        return null;
    }

    @Override
    public void deleteRelationForCurrentUsers(List<Long> userids, String token) {
        for (Long userid : userids) {
            deleteRelationForCurrentUser(Math.toIntExact(userid), token);
        }
    }
}
