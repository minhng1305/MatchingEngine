package com.project.matchingengine.controllers.authentication;

import com.project.matchingengine.controllers.order.OrderController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;


import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import com.project.matchingengine.models.authentication.LoginCredentials;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.utils.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController
{
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private UserRepo userRepo;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepo userRepo,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          PasswordEncoder passwordEncoder)
    {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

//    @PostMapping("register")
//    public Map<String, Object> registerHandler(@RequestBody User user)
//    {
//        String encodedPass = passwordEncoder.encode(user.getPassword());
//        user.setPassword(encodedPass);
//        user = userRepo.save(user);
//
//        String token = jwtUtil.generateToken(user.getUsername());
//        return Collections.singletonMap("jwt-token",token);
//    }

    @PostMapping("/register")
    public ResponseEntity<?> registerHandler(@RequestBody Map<String, String> userData) {
        try {
            String username = userData.get("username");
            String email = userData.get("email");
            String password = userData.get("password");

            // Check if user already exists
            if (userRepo.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Username already exists"
                ));
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));

            user = userRepo.save(user);
            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                    "userId", user.getUserId().toString(),
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Registration failed: " + e.getMessage()
            ));
        }
    }

//    @PostMapping("login")
//    public Map<String,Object> loginHandler(@RequestBody LoginCredentials body)
//    {
//        try
//        {
//            UsernamePasswordAuthenticationToken authInputToken = new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword());
//            authenticationManager.authenticate(authInputToken);
//            logger.info("User "+body.getUsername()+" logged in successfully.");
//            String token = jwtUtil.generateToken(body.getUsername());
//            return Collections.singletonMap("jwt-token",token);
//        } catch(AuthenticationException authExc) {
//            logger.error("User "+body.getUsername()+" failed to log in.");
//            throw new RuntimeException("Invalid username/password.");
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<?> loginHandler(@RequestBody LoginCredentials body) {
        try {
            UsernamePasswordAuthenticationToken authInputToken =
                    new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword());

            authenticationManager.authenticate(authInputToken);
            String token = jwtUtil.generateToken(body.getUsername());

            Optional<User> userOpt = userRepo.findByUsername(body.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "User not found"
                ));
            }

            User user = userOpt.get();

            // Return response matching frontend expectations
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", Map.of(
                    "userId", user.getUserId().toString(),
                    "username", user.getUsername(),
                    "email", user.getEmail()
            ));

            return ResponseEntity.ok(response);
        } catch (AuthenticationException authExc) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid username/password"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Login failed: " + e.getMessage()
            ));
        }
    }
}