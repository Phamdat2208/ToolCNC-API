package com.toolcnc.api.security;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String username) {
        // Timeout set to a large value or Long.MAX_VALUE to avoid frequent reconnections
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L); // 24 hours
        
        // Remove emitter on completion, timeout, or error
        emitter.onCompletion(() -> emitters.remove(username));
        emitter.onTimeout(() -> emitters.remove(username));
        emitter.onError((e) -> emitters.remove(username));
        
        emitters.put(username, emitter);
        
        // Send a dummy event to establish the connection
        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE connection established"));
        } catch (IOException e) {
            emitters.remove(username);
        }
        
        return emitter;
    }

    public void sendLogoutEvent(String username) {
        SseEmitter emitter = emitters.get(username);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("logout").data("{\"action\": \"LOGOUT\"}"));
                emitter.complete(); // Close connection after sending logout
                emitters.remove(username);
            } catch (IOException e) {
                emitters.remove(username);
            }
        }
    }
}
