package com.toke.toke_project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // 1) 비밀번호 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2) AuthenticationManager (로그인 처리에 필요)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 3) Spring Security 필터 체인 설정
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 개발 단계에서는 CSRF 비활성화 (실서비스는 활성 권장)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                		"/register_success", 
                		"/register", 
                		"/forgot/**", 
                		"/css/**", 
                		"/js/**",
                		"/img/**").permitAll() // 누구나 접근 가능
                .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자 전용
                .anyRequest().authenticated() // 그 외는 로그인 필요
            )
            .formLogin(login -> login
                .loginPage("/login") // 로그인 페이지 (커스텀)
                .defaultSuccessUrl("/", false) // 로그인 성공 시 이동할 기본 페이지
                .failureUrl("/login?error=true") // 로그인 실패 시
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true") // 로그아웃 후 이동
                .permitAll()
            );

        return http.build();
    }
    

}

