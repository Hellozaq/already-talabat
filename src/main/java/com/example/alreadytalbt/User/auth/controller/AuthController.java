package com.example.alreadytalbt.User.auth.controller;
import com.example.alreadytalbt.User.Enums.Role;
import com.example.alreadytalbt.User.dto.AuthUserResponseDTO;
import com.example.alreadytalbt.User.auth.JwtUtil;
import com.example.alreadytalbt.User.model.User;
import com.example.alreadytalbt.User.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepo userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String address = body.get("address");
        String phone = body.get("phone");
        String roleStr = body.get("role");

        // Check for existing username
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user  already exists");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid role provided.");
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(password);

        // Create and save user with full details
        User user = new User(username, email, hashedPassword, address, phone, role);
        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getId());

        // Return response DTO
        AuthUserResponseDTO response = new AuthUserResponseDTO(username, email, role, token);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        }

        String token = jwtUtil.generateToken(user.getId());

        AuthUserResponseDTO response = new AuthUserResponseDTO(
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );

        return ResponseEntity.ok(response);
    }

}
