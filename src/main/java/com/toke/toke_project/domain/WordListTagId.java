package com.toke.toke_project.domain;

import java.io.Serializable;
import java.util.Objects;

public class WordListTagId implements Serializable {
	private Long listId;
	private Long tagId;

	public WordListTagId() {
	}

	public WordListTagId(Long listId, Long tagId) {
		this.listId = listId;
		this.tagId = tagId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof WordListTagId))
			return false;
		WordListTagId that = (WordListTagId) o;
		return Objects.equals(listId, that.listId) && Objects.equals(tagId, that.tagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(listId, tagId);
	}

}
