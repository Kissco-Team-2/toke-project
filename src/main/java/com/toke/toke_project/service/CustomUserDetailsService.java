 package com.toke.toke_project.service;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

        String role = normalizeForRoles(u.getRole());
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role));

        return CustomUserDetails.from(u, authorities);
    }

    private String normalizeForRoles(String r) {
        if (r == null) return "USER";
        r = r.trim();
        if (r.toUpperCase().startsWith("ROLE_")) r = r.substring(5);
        return r.toUpperCase(); // admin -> ADMIN, user -> USER
    }
}
