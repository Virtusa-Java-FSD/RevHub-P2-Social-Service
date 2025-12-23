package com.revhub.dto;

import lombok.Data;

@Data
public class CommentRequest {
    private String content;
    private String parentCommentId; // For threaded comments/replies
}
