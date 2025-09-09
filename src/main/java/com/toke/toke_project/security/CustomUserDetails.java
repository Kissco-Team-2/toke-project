package com.toke.toke_project.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

import com.toke.toke_project.domain.Users;

/**
 * Spring Security에서 인증된 사용자 정보를 담는 커스텀 UserDetails 구현체
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;  // 내부 식별자 (PK)
    private final String email; // 로그인 아이디 (username 역할)
    private final String password; // 암호화된 비밀번호
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id,
                             String email,
                             String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * Users 엔티티를 기반으로 CustomUserDetails 생성
     */
    public static CustomUserDetails from(Users u,
                                         Collection<? extends GrantedAuthority> authorities) {
        return new CustomUserDetails(
                u.getId(),          // PK
                u.getEmail(),       // 이메일을 username으로 사용
                u.getPassword(),    // 암호화된 비밀번호
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;  // 스프링 시큐리티에서 username은 email 사용
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 계정 상태 관련 메서드들 (필요시 로직 추가 가능)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
