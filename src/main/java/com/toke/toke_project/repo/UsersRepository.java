package com.toke.toke_project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toke.toke_project.domain.Users;

public interface UsersRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByNickname(String nickname);
}
