package com.revhub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "story_likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoryLike {
    @Id
    private String id;
    private String storyId;
    private Long userId;
    private String userName;

    @CreatedDate
    private LocalDateTime createdAt;
}
