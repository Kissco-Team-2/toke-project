package com.toke.toke_project.web.dto;

import com.toke.toke_project.service.model.QuestionMode;

public record GenerateRequest(
        String category,
        QuestionMode mode,      // null이면 기본 JP_TO_KR
        Integer questionCount   // null 또는 <=0 이면 10
) {}