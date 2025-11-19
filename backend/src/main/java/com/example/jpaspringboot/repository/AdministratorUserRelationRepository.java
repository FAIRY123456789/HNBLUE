/**
 * 管理员-用户关系数据访问层接口
 *
 * 功能概述：
 * • 提供管理员与用户关联关系的数据持久化操作
 * • 基于复合主键实现多对多关系管理
 * • 支持按管理员或用户维度查询关联关系
 *
 * 实体关系：
 * • 管理员(Admin)与用户(User)之间的多对多关联
 * • 使用复合主键AdministratorUserRelationId作为唯一标识
 *
 * 核心查询：
 * • findByAdmin - 查询指定管理员管理的所有用户关系
 * • findByUser - 查询指定用户所属的所有管理员关系
 *
 * 技术特性：
 * • 继承JpaRepository获得基础CRUD操作
 * • 基于方法命名规范自动生成查询语句
 * • 支持关联实体的级联查询操作
 */
package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.Admin;
import com.example.jpaspringboot.entity.AdministratorUserRelation;
import com.example.jpaspringboot.entity.User;
import com.example.jpaspringboot.entity.ids.AdministratorUserRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AdministratorUserRelationRepository extends JpaRepository<AdministratorUserRelation, AdministratorUserRelationId> {

    /**
     * 根据管理员查询关联的用户关系列表
     * @param admin 管理员实体
     * @return 该管理员管理的所有用户关系列表
     */
    List<AdministratorUserRelation> findByAdmin(Admin admin);

    /**
     * 根据用户查询关联的管理员关系列表
     * @param user 用户实体
     * @return 该用户所属的所有管理员关系列表
     */
    List<AdministratorUserRelation> findByUser(User user);

}
