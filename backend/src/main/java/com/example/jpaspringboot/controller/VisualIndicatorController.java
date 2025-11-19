/**
 * 可视化指标数据API控制器
 *
 * 功能概述：
 * • 提供可视化面板指标数据的查询和更新接口
 * • 支持当前年份数据的批量获取
 * • 允许动态更新指定区域的指标数值
 *
 * 接口特性：
 * • RESTful API设计风格
 * • 无状态服务，无需身份验证
 * • 统一的JSON响应格式
 * • 异常处理与错误消息返回
 *
 * 数据流：
 * • 控制器接收HTTP请求 → 服务层处理业务逻辑 → 数据层持久化操作
 * • 返回标准化响应实体，包含成功数据或错误信息
 */
package com.example.jpaspringboot.controller;
import com.example.jpaspringboot.entity.VisualIndicatorData;
import com.example.jpaspringboot.service.VisualIndicatorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/indicators")
public class VisualIndicatorController {

    @Autowired
    private VisualIndicatorService service;

    /**
     * 获取当前年份所有可视化指标数据
     *
     * @return 包含当前年份所有地区指标数据的列表
     * @apiNote 用于仪表盘和数据可视化界面的数据加载
     */
    @GetMapping("/current")
    public ResponseEntity<List<VisualIndicatorData>> getCurrentData() {
        return ResponseEntity.ok(service.getAllCurrentData());
    }

    /**
     * 更新指定地区特定指标数值
     *
     * @param region 地区名称标识
     * @param indicator 指标类型编码
     * @param value 更新后的指标数值
     * @return 更新后的指标数据实体
     * @apiNote 支持动态调整指标数值，无需身份验证
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateIndicator(@RequestParam String region,
                                             @RequestParam String indicator,
                                             @RequestParam double value) {
        try {
            VisualIndicatorData updated = service.updateIndicatorValue(region, indicator, value);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新失败: " + e.getMessage());
        }
    }
}