package com.toke.toke_project.config;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
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
		http.csrf(csrf -> csrf
				// 비밀번호 찾기 흐름만 CSRF 제외
				.ignoringRequestMatchers("/forgot/password/**"))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/words", "/words/**", "/", "/login", "/find_account_modal", "/register_success", "/register",
								"/forgot/**", "/css/**", "/js/**", "/img/**")
						.permitAll()
						.requestMatchers(org.springframework.http.HttpMethod.GET, "/lists", "/lists/*", "/lists/search")
						.permitAll().requestMatchers("/lists/**").authenticated().requestMatchers("/admin/**")
						.hasRole("ADMIN").anyRequest().authenticated())
				.formLogin(login -> login.loginPage("/login").loginProcessingUrl("/login").usernameParameter("email")
						.passwordParameter("password").defaultSuccessUrl("/", false).failureUrl("/login?error=true")
						.permitAll())
				.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").permitAll());

		return http.build();
	}
}