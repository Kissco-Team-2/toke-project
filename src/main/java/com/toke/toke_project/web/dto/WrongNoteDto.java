package com.toke.toke_project.web.dto;

import java.time.LocalDateTime;

public class WrongNoteDto {
	private Long noteId;
	private Long userId;
	private Long wordId;

	private String japaneseWord;
	private String readingKana;
	private String koreanMeaning;
	private String exampleSentenceJp;

	private Long wrongCount;
	private LocalDateTime lastWrongAt;

	private String note;
	private String starred;

	private String category;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public LocalDateTime getNoteCreatedAt() {
		return noteCreatedAt;
	}

	public void setNoteCreatedAt(LocalDateTime noteCreatedAt) {
		this.noteCreatedAt = noteCreatedAt;
	}

	private LocalDateTime noteCreatedAt;

	// getters / setters
	public Long getNoteId() {
		return noteId;
	}

	public void setNoteId(Long noteId) {
		this.noteId = noteId;
	}


	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getWordId() {
		return wordId;
	}
	
	public void setWordId(Long wordId) {
		this.wordId = wordId;
	}

	public String getJapaneseWord() {
		return japaneseWord;
	}

	public void setJapaneseWord(String japaneseWord) {
		this.japaneseWord = japaneseWord;
	}

	public String getReadingKana() {
		return readingKana;
	}

	public void setReadingKana(String readingKana) {
		this.readingKana = readingKana;
	}

	public String getKoreanMeaning() {
		return koreanMeaning;
	}

	public void setKoreanMeaning(String koreanMeaning) {
		this.koreanMeaning = koreanMeaning;
	}

	public String getExampleSentenceJp() {
		return exampleSentenceJp;
	}

	public void setExampleSentenceJp(String exampleSentenceJp) {
		this.exampleSentenceJp = exampleSentenceJp;
	}

	public Long getWrongCount() {
		return wrongCount;
	}

	public void setWrongCount(Long wrongCount) {
		this.wrongCount = wrongCount;
	}

	public LocalDateTime getLastWrongAt() {
		return lastWrongAt;
	}

	public void setLastWrongAt(LocalDateTime lastWrongAt) {
		this.lastWrongAt = lastWrongAt;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getStarred() {
		return starred;
	}

	public void setStarred(String starred) {
		this.starred = starred;
	}
}
