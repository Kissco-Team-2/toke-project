package com.toke.toke_project.web.dto;

import java.time.LocalDate;

public class WrongNoteQuizRequest {
	// either specify dateFrom/dateTo (inclusive), or leave null to ignore
	private String dateFrom; // yyyy-MM-dd
	private String dateTo; // yyyy-MM-dd

	// optional: category filter (exact match to Word.category)
	private String category;

	// desired number of questions
	private Integer count;

	public WrongNoteQuizRequest() {
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(String dateFrom) {
		this.dateFrom = dateFrom;
	}

	public String getDateTo() {
		return dateTo;
	}

	public void setDateTo(String dateTo) {
		this.dateTo = dateTo;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
}
