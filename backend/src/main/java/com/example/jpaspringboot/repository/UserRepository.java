/**
 * 用户数据访问层接口
 *
 * 功能概述：
 * • 基于Spring Data JPA提供用户实体的CRUD操作
 * • 支持自定义查询方法和分页查询
 * • 通过方法命名约定自动生成SQL查询
 *
 * 继承特性：
 * • 继承JpaRepository获得标准CRUD操作
 * • 支持分页和排序功能
 * • 自动实现接口方法，无需编写实现类
 *
 * 查询方法类型：
 * • 派生查询 - 根据方法名自动生成查询
 * • JPQL查询 - 使用@Query注解自定义查询
 * • 原生SQL查询 - 支持原生SQL语句
 *
 * 使用场景：
 * • 用户信息的增删改查操作
 * • 复杂条件查询和分页展示
 * • 用户数据统计和分析
 */
package com.example.jpaspringboot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jpaspringboot.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByIdIn(List<Integer> ids);

    /**
     * 根据用户名精确查询用户
     * 派生查询方法：自动生成 SELECT * FROM user WHERE name = ?
     * @param name 用户名
     * @return 用户实体对象
     */
    User findByName(String name);

    /**
     * 根据邮箱精确查询用户，用于注册重复校验。
     */
    User findByEmail(String email);

    boolean existsByName(String name);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * 根据ID范围查询用户列表
     * 派生查询方法：自动生成 SELECT * FROM user WHERE id BETWEEN ? AND ?
     * @param startId 起始ID
     * @param endId 结束ID
     * @return 用户实体列表
     */
    List<User> findByIdBetween(int startId, int endId);

    /**
     * 根据用户名模糊查询用户列表
     * 派生查询方法：自动生成 SELECT * FROM user WHERE name LIKE %?%
     * @param name 用户名关键词
     * @return 用户实体列表
     */
    List<User> findByNameContaining(String name);

    /**
     * 根据ID列表分页查询用户
     * JPQL查询：自定义查询语句，支持分页
     * @param ids 用户ID列表
     * @param pageable 分页参数
     * @return 分页用户结果
     */
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    Page<User> findAllById(@Param("ids") List<Integer> ids, Pageable pageable);

    /**
     * 在指定ID列表中按用户名关键词搜索并分页
     * JPQL查询：组合条件查询，支持关键词模糊匹配和ID范围限制
     * @param keyword 用户名关键词
     * @param ids 用户ID列表
     * @param pageable 分页参数
     * @return 分页用户结果
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id IN :ids AND (" +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))" )
    Page<User> searchByKeywordInIds(@Param("keyword") String keyword,
                                    @Param("ids") List<Integer> ids,
                                    Pageable pageable);

}
