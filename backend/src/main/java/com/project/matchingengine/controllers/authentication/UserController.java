package com.project.matchingengine.controllers.authentication;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.service.authentication.UserService;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/auth")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("register")
    public User register(@RequestBody User user)
    {
        return service.saveUser(user);
    }
}

