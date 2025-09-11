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

	    		    // --- QnA: 우선 구체 경로를 위로 ---
	    		    .requestMatchers(HttpMethod.GET, "/qna/form").authenticated()       // 폼은 로그인 필요
	    		    .requestMatchers(HttpMethod.POST, "/qna").authenticated()           // 작성
	    		    .requestMatchers(HttpMethod.POST, "/qna/*/reply").hasRole("ADMIN")  // 관리자 답변
	    		    .requestMatchers(HttpMethod.POST, "/qna/*/close").authenticated()   // (있다면) 닫기

	    		    // 목록/상세는 공개 (※ 폼보다 아래에 둬야 함)
	    		    .requestMatchers(HttpMethod.GET, "/qna", "/qna/*").permitAll()

	    		    // --- 모두의 단어장 ---
	    		    .requestMatchers(HttpMethod.GET, "/lists", "/lists/*").authenticated()
	    		    .requestMatchers("/lists/search").authenticated()

	    		    // --- 오답노트 ---
	    		    .requestMatchers("/wrong-notes/**").authenticated()
	    		    .requestMatchers("/api/wrong-notes/**").authenticated()

	    		    // --- 단어/표현 보기·검색 ---
	    		    .requestMatchers(HttpMethod.GET, "/words", "/words/**").permitAll()

	    		    // --- 마이페이지 ---
	    		    .requestMatchers("/mypage/**").authenticated()

	    		    // --- 퀴즈 ---
	    		    .requestMatchers(HttpMethod.GET, "/quiz").authenticated()
	    		    .requestMatchers(HttpMethod.POST, "/quiz/start").authenticated()
	    		    .requestMatchers(HttpMethod.POST, "/quiz/*/submit").authenticated()
	    		    .requestMatchers(HttpMethod.POST, "/api/quiz/*/grade").hasAnyRole("USER","ADMIN")

	    		    // --- 관리자 ---
	    		    .requestMatchers("/admin/**").hasRole("ADMIN")

	    		    // 그 외 전부 인증
	    		    .anyRequest().authenticated()
	    		)
						.formLogin(login -> login.loginPage("/login")
						.loginProcessingUrl("/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.defaultSuccessUrl("/", false)
						.failureUrl("/login?error=true")
						.permitAll())
						
						
						.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout=true").permitAll());


	    return http.build();
	}
}
