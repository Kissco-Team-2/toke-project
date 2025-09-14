package com.toke.toke_project.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.toke.toke_project.service.model.QuizPaper;

@Configuration
public class CacheConfig {
	
	
	// @Configuration 어딘가
	@Bean
	public Cache<String, QuizPaper> quizCache() {
	  return Caffeine.newBuilder()
	      .maximumSize(1000)
	      .expireAfterWrite(Duration.ofMinutes(30))   // ⬅️ 최소 이 정도
	      .build();
	}

}
