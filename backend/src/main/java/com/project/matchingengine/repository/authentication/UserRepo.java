package com.project.matchingengine.repository.authentication;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.matchingengine.models.authentication.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepo extends JpaRepository<User, String>
{
    public Optional<User> findByUsername(String username);
}
