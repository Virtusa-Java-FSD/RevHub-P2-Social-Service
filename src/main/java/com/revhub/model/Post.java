package com.revhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    private String id;
    private String content;
    @Deprecated // Use mediaUrls instead
    private String imageUrl;
    // Multiple media support (images/videos)
    private List<String> mediaUrls = new ArrayList<>();
    private Long authorId;
    private String authorName;
    private String authorUsername;
    private String authorProfilePicture;
    private Integer likesCount = 0;
    private Integer commentsCount = 0;
    private Integer sharesCount = 0;

    @org.springframework.data.annotation.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private boolean isLiked;

    // Visibility control (HP-3)
    private PostVisibility visibility = PostVisibility.PUBLIC;
    // Hashtags (HP-1)
    private List<String> hashtags = new ArrayList<>();
    // Mentions (HP-2) - stores user IDs
    private List<Long> mentions = new ArrayList<>();
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
