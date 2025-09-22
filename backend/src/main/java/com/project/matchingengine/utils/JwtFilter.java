package com.project.matchingengine.utils;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.project.matchingengine.service.authentication.CustomedUserDetailsService;


@Component
public class JwtFilter extends OncePerRequestFilter
{
    private CustomedUserDetailsService userDetailService;
    private JwtUtil jwtUtil;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, CustomedUserDetailsService userDetailService)
    {
        this.jwtUtil = jwtUtil;
        this.userDetailService = userDetailService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if(authHeader!=null && !authHeader.isBlank() && authHeader.startsWith("Bearer")) {
            String jwt = authHeader.substring(7);
            if(jwt == null || jwt.isBlank()){
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Token");
            } else {
                try {
                    String username = jwtUtil.validateTokenAndRetrieveSubject(jwt);
                    UserDetails userDetails = userDetailService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username,userDetails.getPassword(),userDetails.getAuthorities());

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch(JWTVerificationException exc){
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid Token");
                }
            }
        }
        filterChain.doFilter(request,response);
    }
}
