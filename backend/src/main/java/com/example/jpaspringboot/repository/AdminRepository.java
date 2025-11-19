/**
 * 管理员数据访问层接口
 *
 * 功能概述：
 * • 提供管理员实体的基础CRUD操作
 * • 扩展自定义查询方法支持按名称检索
 * • 继承JPA标准接口获得完整数据访问能力
 *
 * 继承特性：
 * • JpaRepository<Admin, Long> - 泛型参数：Admin实体类，Long主键类型
 * • 自动获得save, delete, findById, findAll等标准方法
 *
 * 自定义查询：
 * • findByName - 根据管理员名称精确查询单个管理员
 * • 遵循Spring Data JPA方法命名规范自动生成查询
 *
 * 使用场景：
 * • 管理员登录验证
 * • 管理员信息管理
 * • 权限系统基础数据访问
 */
package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminRepository extends JpaRepository<Admin,Long> {
    /**
     * 根据管理员名称查询管理员信息
     * @param name 管理员名称
     * @return 匹配的管理员实体，未找到时返回null
     */
    Admin findByName(String name);
}
