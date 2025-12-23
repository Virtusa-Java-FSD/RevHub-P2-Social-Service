package com.revhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "stories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Story {
    @Id
    private String id;
    private String imageUrl;
    private String mediaType; // "text", "image", or "video"
    private String caption;
    private Long authorId;
    private String authorName;
    private String authorProfilePicture;
    private Integer likesCount = 0;

    @CreatedDate
    private LocalDateTime createdAt;
    @Indexed(expireAfterSeconds = 86400) // 24 hours
    private LocalDateTime expiresAt;
}
