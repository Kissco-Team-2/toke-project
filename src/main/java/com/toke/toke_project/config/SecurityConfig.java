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

    // 비밀번호 해시
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 처리 매니저
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 인가 규칙
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // --- 완전 공개 ---
                .requestMatchers(
                    "/", "/error",
                    "/login", "/register", "/register_success",
                    "/forgot/**",
                    "/find_account_modal",
                    "/css/**", "/js/**", "/img/**", "/webjars/**"
                ).permitAll()

                // --- QnA ---
                // 목록/상세는 GET 공개
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/qna", "/qna/*").permitAll()
                // 작성(폼/등록), 종료는 인증 필요
                .requestMatchers(org.springframework.http.HttpMethod.GET,  "/qna/new").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/qna").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/qna/*/close").authenticated()
                // 답변은 관리자 전용
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/qna/*/reply").hasRole("ADMIN")

                // --- 모두의 단어장 ---
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/lists", "/lists/*", "/lists/search").permitAll()
                .requestMatchers("/lists/**").authenticated()

                // --- 오답 퀴즈 ---
                .requestMatchers("/wrong-quiz/**").authenticated()

                // --- 마이페이지 ---
                .requestMatchers("/mypage/**").authenticated()

                // --- 관리자 ---
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 그 외 전부 인증
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }
}
