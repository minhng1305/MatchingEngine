package com.project.matchingengine.repository.authentication;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.matchingengine.models.authentication.User;

import java.util.UUID;


public interface UserRepo extends JpaRepository<User, UUID> {

}
