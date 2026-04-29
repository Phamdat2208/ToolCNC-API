package com.toolcnc.api.controller;

import com.toolcnc.api.dto.JwtResponse;
import com.toolcnc.api.dto.LoginRequest;
import com.toolcnc.api.dto.ProfileUpdateRequest;
import com.toolcnc.api.dto.RegisterRequest;
import com.toolcnc.api.model.Role;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.UserRepository;
import com.toolcnc.api.security.CustomUserDetailsService;
import com.toolcnc.api.security.JwtUtil;
import com.toolcnc.api.security.TokenCacheService;
import com.toolcnc.api.security.SseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Autowired
    private SseService sseService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is already in use!"));
        }

        // Create new user's account
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.CUSTOMER) // Default role
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        
        if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
             return ResponseEntity.status(401).body(Map.of("message", "Incorrect username or password"));
        }

        User user = userOpt.get();
        final org.springframework.security.core.userdetails.UserDetails userDetails = 
            userDetailsService.loadUserByUsername(user.getUsername());
            
        if (!loginRequest.isForceLogin() && tokenCacheService.hasActiveSession(user.getUsername())) {
            return ResponseEntity.status(409).body(Map.of(
                    "error_code", "CONCURRENT_LOGIN_DETECTED",
                    "message", "Tài khoản này đang được đăng nhập ở một nơi khác, bạn có muốn tiếp tục?"
            ));
        }

        // If force login, send logout event to previous session
        if (loginRequest.isForceLogin() && tokenCacheService.hasActiveSession(user.getUsername())) {
            sseService.sendLogoutEvent(user.getUsername());
        }

        final String jwt = jwtUtil.generateToken(userDetails);
        tokenCacheService.saveActiveToken(user.getUsername(), jwt, jwtUtil.getJwtExpirationMs());

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            tokenCacheService.invalidateToken(auth.getName());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "email", user.getEmail(),
            "fullName", user.getFullName() != null ? user.getFullName() : "",
            "phone", user.getPhone() != null ? user.getPhone() : "",
            "role", user.getRole().name()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest profileRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        user.setFullName(profileRequest.getFullName());
        user.setPhone(profileRequest.getPhone());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Profile updated successfully!",
            "fullName", user.getFullName(),
            "phone", user.getPhone()
        ));
    }

    @GetMapping("/stream")
    public SseEmitter streamEvents() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new RuntimeException("Unauthorized");
        }
        return sseService.createEmitter(auth.getName());
    }
}
