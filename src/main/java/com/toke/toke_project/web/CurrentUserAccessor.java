
package com.toke.toke_project.web; // 실제 패키지에 맞게 바꿔주세요

import com.toke.toke_project.domain.Users;
import com.toke.toke_project.repo.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component 
@RequiredArgsConstructor
public class CurrentUserAccessor {

    private final UsersRepository usersRepo;

    /**
     * 로그인된 사용자의 닉네임 반환 (없거나 익명일 경우 null)
     */
    public String getNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String principalName = auth.getName(); // 보통 이메일
        Optional<Users> ou = usersRepo.findByEmail(principalName);
        return ou.map(Users::getNickname).orElse(null);
    }
}
