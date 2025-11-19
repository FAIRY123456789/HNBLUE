package com.example.jpaspringboot.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jpaspringboot.entity.User;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
// CRUD refers Create, Read, Update, Delete

public interface UserRepository extends JpaRepository<User, Long> {

    User findByName(String name);//需要手动声明，但不需要定义

    List<User> findByIdBetween(int startId, int endId);

    List<User> findByNameContaining(String name);

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    Page<User> findAllById(@Param("ids") List<Integer> ids, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id IN :ids AND u.name LIKE %:keyword%")
    Page<User> searchByNameInIds(@Param("keyword") String keyword,
                                 @Param("ids") List<Integer> ids,
                                 Pageable pageable);

}
