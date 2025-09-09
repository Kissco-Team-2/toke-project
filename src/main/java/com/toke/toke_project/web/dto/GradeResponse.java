package com.toke.toke_project.web.dto;

import java.util.List;

public record GradeResponse(
		int total,
	    int correct,
	    List<QuestionResult> items
		) {}