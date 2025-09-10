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
        System.out.println("📌 [DEBUG] AuthService.register() 호출됨");
        System.out.println("입력값: username=" + f.getUsername() +
                ", email=" + f.getEmail() +
                ", nickname=" + f.getNickname());

        // 1) 비밀번호 확인
        if (!f.getPassword().equals(f.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 2) 중복 체크
        String email = f.getEmail().trim().toLowerCase();
        if (usersRepo.existsByEmail(email)) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }
        if (usersRepo.existsByNickname(f.getNickname().trim())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }
        if (usersRepo.existsByPhoneNumber(f.getPhoneNumber().trim())) {
            throw new IllegalStateException("이미 가입된 전화번호입니다.");
        }

        // 3) 저장 (비밀번호는 반드시 해싱)
        Users u = new Users();
        u.setUsername(f.getUsername().trim());
        u.setEmail(email);
        u.setPhoneNumber(f.getPhoneNumber().trim());
        u.setNickname(f.getNickname().trim());
        u.setPassword(passwordEncoder.encode(f.getPassword()));
        u.setRole("ROLE_USER"); // ✅ prefix 붙여서 저장

        usersRepo.save(u);
        System.out.println("✅ [DEBUG] DB 저장 성공");
    }
}
