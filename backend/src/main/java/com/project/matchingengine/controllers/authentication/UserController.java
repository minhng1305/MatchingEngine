package com.project.matchingengine.controllers.authentication;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.project.matchingengine.models.authentication.User;
import com.project.matchingengine.repository.authentication.UserRepo;
import org.springframework.security.core.context.SecurityContextHolder;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("api/user")
public class UserController
{
    private UserRepo userRepo;

    @Autowired
    public UserController(UserRepo userRepo)
    {
        this.userRepo = userRepo;
    }

    @GetMapping("info")
    public User getUserDetails()
    {
        String userName = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepo.findByUsername(userName).get();
    }
}

