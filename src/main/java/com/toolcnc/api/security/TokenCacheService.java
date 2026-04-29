package com.toolcnc.api.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenCacheService {

    // Key: username, Value: TokenInfo
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();

    public void saveActiveToken(String username, String token, long expirationMillis) {
        long expireAt = System.currentTimeMillis() + expirationMillis;
        tokenStore.put(username, new TokenInfo(token, expireAt));
    }

    public boolean hasActiveSession(String username) {
        TokenInfo info = tokenStore.get(username);
        if (info == null) return false;
        
        if (System.currentTimeMillis() > info.expireAt) {
            tokenStore.remove(username); // Cleanup expired
            return false;
        }
        return true;
    }

    public boolean isActiveToken(String username, String token) {
        TokenInfo info = tokenStore.get(username);
        if (info == null) return false;
        
        if (System.currentTimeMillis() > info.expireAt) {
            tokenStore.remove(username); // Cleanup expired
            return false;
        }
        
        return token != null && token.equals(info.token);
    }

    public void invalidateToken(String username) {
        tokenStore.remove(username);
    }

    private static class TokenInfo {
        String token;
        long expireAt;

        TokenInfo(String token, long expireAt) {
            this.token = token;
            this.expireAt = expireAt;
        }
    }
}
