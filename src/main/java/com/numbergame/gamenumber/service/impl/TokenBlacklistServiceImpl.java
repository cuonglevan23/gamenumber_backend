package com.numbergame.gamenumber.service.impl;

import com.numbergame.gamenumber.service.ITokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements ITokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public void blacklistToken(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationSeconds, TimeUnit.SECONDS);
        log.info("Token added to blacklist: {}", token.substring(0, Math.min(20, token.length())) + "...");
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist");
    }
}

