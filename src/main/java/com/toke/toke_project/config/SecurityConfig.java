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
		http.csrf(csrf -> csrf.disable()) // REST 호출 위주면 disable 유지
				.authorizeHttpRequests(auth -> auth
						// --- 완전 공개 ---
						.requestMatchers("/", "/error", "/login", "/register", "/register_success", "/forgot/**",
								"/find_account_modal", "/css/**", "/js/**", "/img/**", "/webjars/**")
						.permitAll()

						// --- QnA ---
						.requestMatchers(HttpMethod.GET, "/qna", "/qna/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/qna/new").authenticated()
						.requestMatchers(HttpMethod.POST, "/qna").authenticated()
						.requestMatchers(HttpMethod.POST, "/qna/*/close").authenticated()
						.requestMatchers(HttpMethod.POST, "/qna/*/reply").hasRole("ADMIN")

						// --- 모두의 단어장 ---
						.requestMatchers(HttpMethod.GET, "/lists", "/lists/*", "/lists/search").permitAll()
						.requestMatchers("/lists/**").authenticated()

						// --- 오답 퀴즈 ---
						.requestMatchers("/wrong-quiz/**").authenticated()

						// --- 마이페이지 ---
						.requestMatchers("/mypage/**").authenticated()

						// --- 퀴즈 API 추가 ---
						// 관리자 생성: /admin/quiz/generate
						.requestMatchers(HttpMethod.POST, "/admin/quiz/generate").hasRole("ADMIN")
						// 사용자 채점: /quiz/{quizId}/grade
						.requestMatchers(HttpMethod.POST, "/quiz/**/grade").hasAnyRole("USER", "ADMIN")
						// (만약 퀴즈 조회/시작용 GET을 추가하면 여기에 GET 규칙도 추가)

						// --- 관리자 ---
						.requestMatchers("/admin/**").hasRole("ADMIN")

						// 그 외 전부 인증
						.anyRequest().authenticated())
				.formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email")
						.passwordParameter("password").defaultSuccessUrl("/", false).failureUrl("/login?error=true")
						.permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").permitAll());

		return http.build();
	}
	

	
}
