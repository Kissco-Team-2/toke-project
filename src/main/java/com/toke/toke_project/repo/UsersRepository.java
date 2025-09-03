package com.toke.toke_project.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.toke.toke_project.domain.Users;

public interface UsersRepository extends JpaRepository<Users, Long> { //이미 존재하는 이메일, 별명인지 확인
	Optional<Users> findByEmail(String email);
	Optional<Users> findByUsernameAndPhoneNumber(String username, String phoneNumber);
	boolean existsByPhoneNumber(String phoneNumber);
	boolean existsByEmail(String email);
	boolean existsByNickname(String nickname);
	Optional<Users> findByUsernameAndPhoneNumberAndEmail(String username, String phoneNumber, String email);
	
}
