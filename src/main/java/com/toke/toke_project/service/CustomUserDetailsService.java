package com.toke.toke_project.service;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users u = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));

        return User.withUsername(u.getEmail())
                   .password(u.getPassword())
                   .roles(u.getRole())   // "user" -> ROLE_USER
                   .build();
    }
}


