package com.toke.toke_project.service;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import com.toke.toke_project.web.dto.RegisterForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsersRepository usersRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterForm f) {
        // 1) 비밀번호 확인
        if (!f.getPassword().equals(f.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 2) 중복 체크
        if (usersRepo.existsByEmail(f.getEmail().trim())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        if (usersRepo.existsByNickname(f.getNickname().trim())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
        

        // 3) 저장 (비밀번호는 반드시 해싱)
        Users u = new Users();
        u.setUsername(f.getUsername());
        u.setEmail(f.getEmail().trim());
        u.setNickname(f.getNickname().trim());
        u.setPassword(passwordEncoder.encode(f.getPassword()));
        u.setRole("user");

        usersRepo.save(u);
    }
}
