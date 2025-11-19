package com.example.jpaspringboot.util;

import java.util.Random;

public class FuzzUtil {


    private static final Random random = new Random();

    // 生成随机字符串
    public static String randomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 生成超长字符串
    public static String longString(int length) {
        return "A".repeat(Math.max(0, length));
    }

    // 生成随机数字
    public static double randomDouble() {
        return new Random().nextDouble() * 1e10 - 5e9;
    }

    // 生成随机负数
    public static double randomNegative() {
        return -Math.abs(new Random().nextDouble() * 100);
    }

    // 生成用于逻辑绕过的 SQL 注入字符串
    public static String sqlInjection() {
        return "' OR '1'='1";
    }

    // 生成随机非法用户名
    public static String randomFuzzUsername() {
        String[] invalidUsernames = {
                "",                                 // 空字符串
                " ",                                // 空格
                "user@",                            // 特殊符号
                "用户",                              // 中文字符
                "ThisIsAVeryLongUsernameExceedingMaxLength",
                "!@#$%^&*()",                        // 全特殊字符
                "null", "undefined"                   // 特殊词汇
        };
        return invalidUsernames[random.nextInt(invalidUsernames.length)];
    }

    // 生成随机非法密码
    public static String randomFuzzPassword() {
        String[] invalidPasswords = {
                "",                                 // 空字符串
                "123",                              // 太短
                "password",                         // 常见弱密码
                "pa$$w0rd!",                        // 特殊符号组合
                " ",                                // 空格
                "ThisIsAVeryLongPasswordExceedingMaxLength",
                "!@#$%^&*()",                        // 全特殊字符
                "qwerty", "abc123"                   // 弱密码
        };
        return invalidPasswords[random.nextInt(invalidPasswords.length)];
    }

    // 生成随机非法邮箱
    public static String randomFuzzEmail() {
        String[] invalidEmails = {
                "plainaddress",                     // 无@
                "missingdomain@",                   // 无域名
                "@missingusername.com",             // 无用户名
                "invalid@.com",                     // 点号在@后
                "name@domain..com",                 // 连续点号
                "name@domain.c",                    // 域名过短
                "name@domain@domain.com",           // 多个@
                "user#domain.com"                   // 错误符号
        };
        return invalidEmails[random.nextInt(invalidEmails.length)];
    }

    // 生成随机非法日期
    public static String randomFuzzDate() {
        String[] invalidDates = {
                "2023-02-30",                       // 非法日期
                "0000-00-00",                       // 全零日期
                "2023-13-01",                       // 月份超界
                "2023-05-32",                       // 日期超界
                "20-01-2023",                       // 格式错误
                "abcdefg",                          // 字符串代替日期
                "2024/02/02",                       // 错误分隔符
                "2024-02-30T00:00:00"               // 时间戳格式
        };
        return invalidDates[random.nextInt(invalidDates.length)];
    }
}
