package com.toke.toke_project.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // 👈 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true) // 👈 @PreAuthorize 사용하려면 필요
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

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    http
	      .csrf(csrf -> csrf.disable())
	      .authorizeHttpRequests(auth -> auth
	          // --- 완전 공개 ---
	          .requestMatchers("/", "/error", "/login", "/register", "/register_success",
	                           "/forgot/**", "/find_account_modal",
	                           "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()

	          // --- QnA (목록/상세 공개, 작성/답변/닫기 제한) ---
	          .requestMatchers(HttpMethod.GET, "/qna", "/qna/*").permitAll()
	          .requestMatchers(HttpMethod.GET, "/qna/new").authenticated()
	          .requestMatchers(HttpMethod.POST, "/qna").authenticated()
	          .requestMatchers(HttpMethod.POST, "/qna/*/close").authenticated()
	          .requestMatchers(HttpMethod.POST, "/qna/*/reply").hasRole("ADMIN")

	          // --- 모두의 단어장(리스트 기능은 공개) ---
	          .requestMatchers(HttpMethod.GET, "/lists", "/lists/*", "/lists/search").permitAll()
	          .requestMatchers("/lists/**").authenticated()  // mine/편집/삭제 등은 로그인

	          // --- 단어장/표현 목록 보기·검색 (여기가 빠져서 다 막혔던 부분) ---
	          .requestMatchers(HttpMethod.GET, "/words", "/words/**").permitAll()

	          // --- 마이페이지 ---
	          .requestMatchers("/mypage/**").authenticated()

	          // --- 퀴즈 UI/API: 로그인 필요(정책 유지) ---
	          .requestMatchers(HttpMethod.GET,  "/quiz").authenticated()
	          .requestMatchers(HttpMethod.POST, "/quiz/start").authenticated()
	          .requestMatchers(HttpMethod.POST, "/quiz/*/submit").authenticated()
	          .requestMatchers(HttpMethod.POST, "/api/quiz/*/grade").hasAnyRole("USER","ADMIN")

	          // --- 오답노트: 로그인 필요(정책 유지) ---
	          .requestMatchers("/wrong-notes/**").authenticated()
	          .requestMatchers("/api/wrong-notes/**").authenticated()

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
