package com.toke.toke_project.config;

import com.toke.toke_project.domain.Word;
import com.toke.toke_project.repo.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WordIndexInitializer implements CommandLineRunner {

    private final WordRepository repo;

    // 실행 여부 플래그 (true → 실행 / false → 건너뜀)
    private static final boolean RUN_ON_STARTUP = true;

    @Override
    public void run(String... args) {
        if (!RUN_ON_STARTUP) {
            System.out.println("[WordIndexInitializer] 실행 안 함 (RUN_ON_STARTUP=false)");
            return;
        }

        System.out.println("[WordIndexInitializer] 시작: 전체 Word 데이터 인덱스 갱신");

        List<Word> all = repo.findAll();
        for (Word w : all) {
            w.recalcIndexes(); // ko/ja group, index 자동 계산
        }
        repo.saveAll(all);

        System.out.println("[WordIndexInitializer] 완료: " + all.size() + "건 갱신");
    }
}
