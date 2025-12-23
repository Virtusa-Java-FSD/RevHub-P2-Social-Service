package com.revhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorProfilePicture;
    private String postId;

    // Threaded comments support
    private String parentCommentId; // null for top-level comments
    private Integer repliesCount = 0; // Number of direct replies
    private Integer depth = 0; // Nesting level (0 for top-level)

    @CreatedDate
    private LocalDateTime createdAt;
}
