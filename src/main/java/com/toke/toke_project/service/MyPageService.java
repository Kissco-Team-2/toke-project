package com.toke.toke_project.service;

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final UsersRepository usersRepo;

    @Transactional(readOnly = true)
    public Users getMe(Long id) {
        return usersRepo.findById(id).orElseThrow();
    }

    @Transactional
    public void updateMe(Long id, String email, String nickname, String phone) {
        if (usersRepo.existsByEmailAndIdNot(email, id))
            throw new IllegalStateException("이미 사용 중인 이메일입니다.");
        if (usersRepo.existsByNicknameAndIdNot(nickname, id))
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        if (usersRepo.existsByPhoneNumberAndIdNot(phone, id))
            throw new IllegalStateException("이미 사용 중인 전화번호입니다.");

        Users me = usersRepo.findById(id).orElseThrow();
        me.setEmail(email);
        me.setNickname(nickname);
        me.setPhoneNumber(phone);
    }

    @Transactional
    public void deleteMe(Long id) {
        usersRepo.deleteById(id);
    }
}
