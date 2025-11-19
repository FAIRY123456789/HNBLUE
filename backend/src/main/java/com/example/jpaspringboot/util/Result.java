/**
 * 统一API响应结果封装类
 *
 * 功能概述：
 * • 标准化REST API接口响应格式
 * • 提供链式调用支持便于结果构建
 * • 统一成功/失败状态码和消息管理
 *
 * 响应结构：
 * • success - 操作成功状态标识
 * • code - 业务状态码
 * • message - 响应消息描述
 * • data - 响应数据负载
 *
 * 设计模式：
 * • 使用静态工厂方法创建成功/失败结果
 * • 支持建造者模式的链式调用
 * • 封装内部数据结构保证一致性
 */
package com.example.jpaspringboot.util;
import java.util.HashMap;
import java.util.Map;

//统一返回结果的类
public class Result {

    private Boolean success;

    private Integer code;

    private String message;

    private Map<String, Object> data = new HashMap<String, Object>();

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     */
    private Result() {}

    /**
     * 创建成功响应结果
     * @return 预配置的成功结果实例
     */
    public static Result ok() {
        Result r = new Result();
        r.setSuccess(true);
        r.setCode(ResultCode.SUCCESS);
        r.setMessage("成功");
        return r;
    }

    /**
     * 创建失败响应结果
     * @return 预配置的失败结果实例
     */
    public static Result error() {
        Result r = new Result();
        r.setSuccess(false);
        r.setCode(ResultCode.ERROR);
        r.setMessage("失败");
        return r;
    }

    public Result success(Boolean success){
        this.setSuccess(success);
        return this;
    }

    public Result message(String message){
        this.setMessage(message);
        return this;
    }

    public Result code(Integer code){
        this.setCode(code);
        return this;
    }

    public Result data(String key, Object value){
        this.data.put(key, value);
        return this;
    }

    public Result data(Map<String, Object> map){
        this.setData(map);
        return this;
    }
}
