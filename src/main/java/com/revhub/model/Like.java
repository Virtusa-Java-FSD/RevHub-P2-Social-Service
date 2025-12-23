package com.revhub.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Document(collection = "likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Like {
    @Id
    private String id;
    private Long userId;
    private String userName;
    private String postId;
    @CreatedDate
    private LocalDateTime createdAt;
}
