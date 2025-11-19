package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AdministratorUserRelationRepository extends JpaRepository<AdministratorUserRelation, AdministratorUserRelationId> {
    List<AdministratorUserRelation> findByAdmin(Admin admin);

    List<AdministratorUserRelation> findByUser(User user);

}
