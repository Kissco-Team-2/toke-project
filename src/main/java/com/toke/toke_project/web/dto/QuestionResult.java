package com.toke.toke_project.web.dto;

import java.util.List;

public record QuestionResult(
    int index,
    String prompt,
    List<String> options,
    Integer userSelected, // null 가능
    int correctIndex,
    boolean isCorrect,
    String explain,              // 정답 전체 해설
    String exampleSentenceJp,    // 예문
    List<String> optionExplanations // ★ 각 선지별 해설 (options와 동일 길이)
) {}
