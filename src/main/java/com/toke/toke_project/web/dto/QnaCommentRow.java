package com.toke.toke_project.web.dto;

import java.time.LocalDateTime;

public record QnaCommentRow(
     Long id,
     String authorDisplay,
     boolean isAdmin,
     String content,
     LocalDateTime createdAt
) {}
