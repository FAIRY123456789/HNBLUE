package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin,Long> {
    Admin findByName(String name);
}
