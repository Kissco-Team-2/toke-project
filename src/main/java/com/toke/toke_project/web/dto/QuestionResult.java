package com.toke.toke_project.web.dto;

import java.util.List;

public record QuestionResult(
    int index,
    String prompt,
    List<String> options,
    Integer userSelected, // null 가능
    int correctIndex,
    boolean isCorrect,
    String explain,
    String exampleSentenceJp
) {}
