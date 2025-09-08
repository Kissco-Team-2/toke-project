package com.toke.toke_project.web.dto;

import java.util.List;

/**
 * Bulk delete API 응답 DTO
 */
public class BulkDeleteResponse {
    private List<Long> deletedIds;
    private List<Long> notFoundIds;
    private List<Long> notOwnedIds;
    private String message;

    public BulkDeleteResponse() {}

    public BulkDeleteResponse(List<Long> deletedIds, List<Long> notFoundIds, List<Long> notOwnedIds, String message) {
        this.deletedIds = deletedIds;
        this.notFoundIds = notFoundIds;
        this.notOwnedIds = notOwnedIds;
        this.message = message;
    }

    public List<Long> getDeletedIds() {
        return deletedIds;
    }

    public void setDeletedIds(List<Long> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public List<Long> getNotFoundIds() {
        return notFoundIds;
    }

    public void setNotFoundIds(List<Long> notFoundIds) {
        this.notFoundIds = notFoundIds;
    }

    public List<Long> getNotOwnedIds() {
        return notOwnedIds;
    }

    public void setNotOwnedIds(List<Long> notOwnedIds) {
        this.notOwnedIds = notOwnedIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
