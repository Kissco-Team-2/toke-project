package com.toke.toke_project.repo;
//태그 normalized 값으로 중복 확인 후 재사용.
import com.toke.toke_project.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByNormalized(String normalized);
}

