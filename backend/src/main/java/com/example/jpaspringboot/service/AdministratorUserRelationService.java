package com.example.jpaspringboot.service;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;

import java.util.List;

public interface AdministratorUserRelationService {

    void deleteRelationForCurrentUser(Integer userId, String jwtToken);

    AdministratorUserRelation createRelationForCurrentUser(Integer userId, String jwtToken);

    List<User> findUsersByAdmin(Admin admin);

    void deleteRelationForCurrentUsers(List<Long> userids, String token);
}
