package com.project.matchingengine.service.authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.project.matchingengine.repository.authentication.UserRepo;
import com.project.matchingengine.models.authentication.User;

@Service
public class CustomedUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepo.findUserByEmail(email);

        if (user == null) {
            System.out.println("User 404");
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }
}
