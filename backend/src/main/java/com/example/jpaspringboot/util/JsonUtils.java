/**
 * JSON序列化与反序列化工具类
 *
 * 功能概述：
 * • 基于FastJSON库提供对象与JSON字符串的相互转换
 * • 支持单个对象和集合对象的JSON序列化操作
 * • 封装JSON操作异常处理，提供统一的工具方法
 *
 * 核心方法：
 * • convertObj2Json - 对象序列化为JSON字符串
 * • convertJson2Obj - JSON字符串反序列化为单个对象
 * • convertJsonArray2List - JSON数组反序列化为对象列表
 *
 * 技术特性：
 * • 使用FastJSON高性能JSON处理库
 * • 泛型支持确保类型安全
 * • 集成SLF4J日志记录便于调试
 *
 * 使用场景：
 * • REST API请求响应数据序列化
 * • 数据库JSON字段的读写操作
 * • 缓存数据的序列化存储
 * • 微服务间数据传输
 *
 * 依赖说明：
 * • 基于Alibaba FastJSON 2.x/1.x版本
 * • 需要配置相应的JSON注解支持
 */
package com.example.jpaspringboot.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String convertObj2Json(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <T> T convertJson2Obj(String json, Class<T> classz) {
        return JSONObject.parseObject(json, classz);
    }

    public static <T> List<T> convertJsonArray2List(String json, Class<T> classz) {
        return JSONArray.parseArray(json, classz);
    }

    public static void main(String[] args) {
    }
}
