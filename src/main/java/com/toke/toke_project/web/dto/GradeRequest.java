package com.toke.toke_project.web.dto;

import java.util.Map;

public record GradeRequest(
		 Map<Integer, Integer> answers
		 ) {

}
