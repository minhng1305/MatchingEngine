package com.project.matchingengine.controllers.authentication;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;

import com.project.matchingengine.models.authentication.LoginCredentials;
import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.utils.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController
{
    private UserRepo userRepo;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepo userRepo, JwtUtil jwtUtil, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder)
    {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("register")
    public Map<String, Object> registerHandler(@RequestBody User user)
    {
        String encodedPass = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPass);
        user = userRepo.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return Collections.singletonMap("jwt-token",token);
    }

    @PostMapping("login")
    public Map<String,Object> loginHandler(@RequestBody LoginCredentials body)
    {
        try
        {
            UsernamePasswordAuthenticationToken authInputToken = new UsernamePasswordAuthenticationToken(body.getUsername(), body.getPassword());
            authenticationManager.authenticate(authInputToken);

            String token = jwtUtil.generateToken(body.getUsername());
            return Collections.singletonMap("jwt-token",token);
        } catch(AuthenticationException authExc) {
            throw new RuntimeException("Invalid username/password.");
        }

    }
}