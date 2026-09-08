/**
 * Redis缓存配置类
 *
 * 功能概述：
 * • 配置RedisTemplate序列化规则，支持对象存储
 * • 定义缓存管理器(CacheManager)和键生成策略
 * • 启用Spring缓存注解支持
 * • 配置缓存过期时间和序列化方案
 *
 * 核心配置：
 * • RedisTemplate - Redis操作模板配置
 * • CacheManager - 缓存管理器配置
 * • KeyGenerator - 自定义缓存键生成规则
 *
 * 序列化策略：
 * • Key使用StringRedisSerializer字符串序列化
 * • Value使用GenericJackson2JsonRedisSerializer JSON序列化
 * • 支持类型信息保存，避免反序列化类型丢失
 *
 * 缓存特性：
 * • 默认600秒缓存过期时间
 * • 支持空值缓存禁用
 * • 可扩展随机TTL防止缓存雪崩
 */
package com.example.jpaspringboot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import jakarta.annotation.Resource;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import java.lang.reflect.Method;
import java.time.Duration;

@Configuration
@EnableCaching  // 启用Spring缓存注解支持
public class RedisConfig {

    /**
     * 自定义缓存键生成规则
     * 生成规则：类全限定名 + 方法名 + 所有参数值
     *
     * @return KeyGenerator实例
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());  // 目标类名
                sb.append(method.getName());             // 方法名
                for (Object obj : params) {
                    sb.append(obj.toString());           // 参数值
                }
                return sb.toString();
            }
        };
    }

    /**
     * 配置RedisTemplate序列化规则
     * 设置Key和Value的序列化方式，支持对象存储
     *
     * @param redisConnectionFactory Redis连接工厂
     * @return 配置完成的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 配置Jackson序列化器
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);  // 所有字段可见
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,
                ObjectMapper.DefaultTyping.NON_FINAL);  // 启用类型信息

        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(om);

        // 设置值序列化器
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);

        // 设置键序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());  // 字符串序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 生成随机TTL（缓存过期时间）
     * 防止缓存同时过期导致的缓存雪崩问题
     *
     * @return 随机的过期时间Duration对象
     */
    private Duration randomTtl() {
        int baseTtl = 600;  // 基础TTL：600秒
        int randomRange = 120;  // 随机范围：±120秒
        int randomTtl = baseTtl + ThreadLocalRandom.current().nextInt(-randomRange, randomRange);
        return Duration.ofSeconds(randomTtl);
    }

    /**
     * 配置缓存管理器
     * 定义缓存序列化规则和过期时间策略
     *
     * @param factory Redis连接工厂
     * @return 配置完成的CacheManager实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();

        // 配置Jackson对象映射器
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance ,
                ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(om);

        // 配置缓存序列化规则。所有业务查询缓存统一放入 hnblue:cache:v2: 命名空间。
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(cacheName -> "hnblue:cache:v2:" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> ttlByCache = new HashMap<>();
        ttlByCache.put("dashboard-summary", config.entryTtl(Duration.ofMinutes(10)));
        ttlByCache.put("sources", config.entryTtl(Duration.ofHours(6)));
        ttlByCache.put("mangrove-cover", config.entryTtl(Duration.ofMinutes(30)));
        ttlByCache.put("region-metrics", config.entryTtl(Duration.ofMinutes(30)));
        ttlByCache.put("literature-carbon", config.entryTtl(Duration.ofHours(2)));
        ttlByCache.put("region-overview", config.entryTtl(Duration.ofMinutes(15)));
        ttlByCache.put("ai-context", config.entryTtl(Duration.ofMinutes(10)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(ttlByCache)
                .transactionAware()
                .build();
        return cacheManager;
    }

    // 注释掉的Hibernate代理序列化配置
    // 用于解决Hibernate懒加载对象的序列化问题
    /*
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.failOnUnknownProperties(false);
        builder.serializerByType(HibernateProxy.class, new JsonSerializer<HibernateProxy>() {
            @Override
            public void serialize(HibernateProxy value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    Hibernate.initialize(value);
                    Object deProxied = ((HibernateProxy) value).getHibernateLazyInitializer().getImplementation();
                    gen.writeObject(deProxied);
                } else {
                    gen.writeObject(value);
                }
            }
        });
        return builder;
    }
    */
}
