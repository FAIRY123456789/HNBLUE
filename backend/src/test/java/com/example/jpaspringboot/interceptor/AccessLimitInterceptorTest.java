package com.example.jpaspringboot.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.jpaspringboot.service.AccessLimit;

class AccessLimitInterceptorTest {

    private final AccessLimitInterceptor interceptor = new AccessLimitInterceptor();

    @Test
    void usesNginxRealIpWhenDirectPeerIsIpv4Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.42");

        assertEquals("203.0.113.42", interceptor.resolveClientAddress(request));
    }

    @Test
    void usesNginxRealIpWhenDirectPeerIsIpv6Loopback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        request.addHeader("X-Real-IP", "2001:db8::42");

        assertEquals("2001:db8::42", interceptor.resolveClientAddress(request));
    }

    @Test
    void ignoresSpoofedHeaderFromNonLoopbackPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("X-Real-IP", "203.0.113.42");

        assertEquals("198.51.100.7", interceptor.resolveClientAddress(request));
    }

    @Test
    void rejectsHostNamesAndMalformedForwardedValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "attacker.example");

        assertEquals("127.0.0.1", interceptor.resolveClientAddress(request));
    }

    @Test
    void rejectsWhenAtomicRedisScriptReturnsBlockedDecision() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-1L);
        ReflectionTestUtils.setField(interceptor, "stringRedisTemplate", redis);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, limitedHandler()));
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("请求过于频繁"));
    }

    @Test
    void allowsWhenAtomicRedisScriptReturnsAcceptedCount() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(10L);
        ReflectionTestUtils.setField(interceptor, "stringRedisTemplate", redis);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), limitedHandler()));
    }

    @Test
    void failsOpenWhenRedisScriptThrows() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("redis unavailable"));
        ReflectionTestUtils.setField(interceptor, "stringRedisTemplate", redis);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), limitedHandler()));
    }

    private HandlerMethod limitedHandler() throws NoSuchMethodException {
        RateLimitedFixture fixture = new RateLimitedFixture();
        return new HandlerMethod(fixture, RateLimitedFixture.class.getMethod("limited"));
    }

    static class RateLimitedFixture {
        @AccessLimit(seconds = 3, maxCount = 10)
        public void limited() {
        }
    }
}
