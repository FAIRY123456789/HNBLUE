/**
 * 可视化指标数据服务实现类
 *
 * 功能概述：
 * • 提供可视化指标数据的查询和更新服务
 * • 管理当前年度指标数据的业务逻辑
 * • 确保数据更新的事务一致性
 *
 * 业务规则：
 * • 仅支持当前年度（2024年）数据的操作
 * • 指标更新仅限于已存在记录，禁止新增
 * • 严格的地区-指标-年份联合校验
 *
 * 数据约束：
 * • 地区名称、指标键值、年份三要素唯一确定记录
 * • 更新操作需确保记录存在性
 * • 事务管理保障数据一致性
 */
package com.example.jpaspringboot.service.impl;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.repository.VisualIndicatorRepository;
import com.example.jpaspringboot.service.VisualIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VisualIndicatorServiceImpl implements VisualIndicatorService {

    // 当前业务年度配置
    private static final int CURRENT_YEAR = 2024;

    @Autowired
    private VisualIndicatorRepository repository;

    /**
     * 获取当前年度所有可视化指标数据
     * @return 当前年度指标数据列表
     */
    @Override
    public List<VisualIndicatorData> getAllCurrentData() {
        return repository.findByYear(CURRENT_YEAR);
    }

    /**
     * 更新指定地区指标的数值
     * 仅支持更新已存在记录，禁止新增数据
     *
     * @param regionName 地区名称
     * @param indicatorKey 指标键值
     * @param value 更新后的指标数值
     * @return 更新后的指标数据实体
     * @throws RuntimeException 当指定记录不存在时抛出异常
     */
    @Override
    @Transactional
    public VisualIndicatorData updateIndicatorValue(String regionName, String indicatorKey, double value) {
        // 根据地区名称、指标键值和年份查询唯一记录
        VisualIndicatorData data = repository.findByRegionNameAndIndicatorKeyAndYear(regionName, indicatorKey, CURRENT_YEAR);
        if (data != null) {
            // 更新指标数值并保存
            data.setValue(value);
            return repository.save(data);
        } else {
            // 记录不存在时抛出业务异常
            throw new RuntimeException("指定地区或指标不存在！只允许更新，不允许新增。");
        }
    }
}