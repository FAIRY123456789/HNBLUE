/**
 * FuzzTest 类用于对系统接口进行模糊测试（Fuzz Testing）。
 *
 * 测试目的：
 * - 验证接口在面对异常输入时的鲁棒性和安全性。
 * - 通过构造非法字符、随机输入和特殊符号，检测接口是否返回合理的错误响应。
 * - 防止由于意外或恶意输入引发的系统崩溃和数据泄露。
 *
 * 测试范围：
 * - 登录接口（/api/login）
 * - 注册接口（/api/register）
 * - 指标更新接口（/api/indicators/update）
 * - 区域详情接口（/api/devisual/{region}）
 * - 特殊字符输入测试（如SQL注入和脚本攻击）
 *
 * 测试框架：
 * - 使用 MockMvc 进行接口请求和响应校验。
 * - 自动化运行测试并验证响应状态是否在客户端错误范围 (4xx)。
 */
package com.example.jpaspringboot.fuzz;

import com.example.jpaspringboot.util.FuzzUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FuzzTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 模糊测试登录接口 (/api/login)。
     *
     * 测试目的：
     * - 检查登录接口在接收非法或恶意输入时的响应情况。
     * - 验证是否能正确返回客户端错误状态码 (4xx)。
     *
     * 测试方法：
     * - 随机生成非法用户名和 SQL 注入型密码。
     * - 使用 POST 方法发送登录请求。
     * - 期望：返回 4xx 错误状态。
     */
    @Test
    public void fuzzLogin() throws Exception {
        for (int i = 0; i < 50; i++) {
            String username = FuzzUtil.randomFuzzUsername();  // 随机非法用户名
            String password = FuzzUtil.sqlInjection();        // 注入型密码：尝试绕过验证

            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}"))
                    .andExpect(status().is4xxClientError());  // 应当返回 4xx，表示验证失败
        }
    }

    /**
     * 模糊测试注册接口 (/api/register)。
     *
     * 测试目的：
     * - 验证注册接口在接收非法字符、超长输入及异常数据时的处理能力。
     *
     * 测试方法：
     * - 随机生成非法用户名、密码、邮箱和出生日期。
     * - 使用 POST 方法发送注册请求。
     * - 期望：返回 4xx 错误状态。
     */
    @Test
    public void fuzzRegister() throws Exception {
        for (int i = 0; i < 50; i++) {
            String username = FuzzUtil.randomFuzzUsername();  // 生成非法用户名
            String password = FuzzUtil.randomFuzzPassword();  // 生成非法密码
            String email = FuzzUtil.randomFuzzEmail();        // 生成非法邮箱
            String birthdate = FuzzUtil.randomFuzzDate();     // 生成非法日期

            mockMvc.perform(post("/api/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + username + "\", \"password\":\"" + password + "\", "
                                    + "\"email\":\"" + email + "\", \"birthdate\":\"" + birthdate + "\"}"))
                    .andExpect(status().is4xxClientError());  // 验证返回4xx状态
        }
    }

    /**
     * 模糊测试指标更新接口 (/api/indicators/update)。
     *
     * 测试目的：
     * - 检查指标更新接口在接收随机非法值时的响应和容错能力。
     *
     * 测试方法：
     * - 随机生成非法区域名、指标名及浮点数值。
     * - 使用 PUT 方法发送更新请求。
     * - 期望：返回 4xx 错误状态。
     */
    @Test
    public void fuzzUpdateIndicator() throws Exception {
        for (int i = 0; i < 50; i++) {
            String region = FuzzUtil.randomString(10);  // 随机区域名
            String indicator = FuzzUtil.randomString(5);  // 随机指标名
            double value = FuzzUtil.randomDouble();       // 随机浮点值

            mockMvc.perform(put("/api/indicators/update")
                            .param("region", region)
                            .param("indicator", indicator)
                            .param("value", String.valueOf(value)))
                    .andExpect(status().is4xxClientError());  // 验证返回4xx状态
        }
    }

    /**
     * 模糊测试区域详情接口 (/api/devisual/{region})。
     *
     * 测试目的：
     * - 检查区域详情接口在接收超长字符串作为区域名时的异常处理。
     *
     * 测试方法：
     * - 生成超长字符串作为区域名（1000字符）。
     * - 使用 GET 方法发送请求。
     * - 期望：返回 4xx 错误状态。
     */
    @Test
    public void fuzzRegionDetail() throws Exception {
        for (int i = 0; i < 50; i++) {
            String region = FuzzUtil.longString(1000);  // 生成超长区域名

            mockMvc.perform(get("/api/devisual/" + region))
                    .andExpect(status().is4xxClientError());  // 验证返回4xx状态
        }
    }

    /**
     * 模糊测试非法字符输入。
     *
     * 测试目的：
     * - 检查接口在面对非法字符（如脚本注入、SQL注入、特殊符号）时的鲁棒性。
     *
     * 测试方法：
     * - 构造常见非法字符数组（包括脚本、SQL和特殊符号）。
     * - 逐个作为用户名和密码进行登录请求。
     * - 期望：返回 4xx 错误状态。
     */
    @Test
    public void fuzzIllegalCharacters() throws Exception {
        String[] illegalInputs = {
                "<script>alert(1)</script>",  // 脚本注入
                "\" OR 1=1 --",              // SQL注入
                "DROP TABLE users;",         // SQL删除语句
                "😉🐍💥"                      // 特殊符号
        };

        for (String input : illegalInputs) {
            mockMvc.perform(post("/api/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"" + input + "\", \"password\":\"" + input + "\"}"))
                    .andExpect(status().is4xxClientError());  // 验证返回4xx状态
        }
    }
}
