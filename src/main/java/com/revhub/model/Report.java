package com.revhub.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Document(collection = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @Id
    private String id;
    private Long reporterId;
    private String reporterName;
    private ReportType type;
    // ID of the reported entity (user ID or post ID)
    private String reportedEntityId;
    private String reportedEntityType; // "USER" or "POST" or "COMMENT"
    private String reason;
    private String description;
    private ReportStatus status = ReportStatus.PENDING;
    private Long resolvedBy; // Admin user ID who resolved
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    @CreatedDate
    private LocalDateTime createdAt;
    private ActionTaken actionTaken = ActionTaken.NONE;
    public enum ActionTaken {
        NONE,
        USER_BLOCKED,
        USER_DELETED,
        POST_DELETED,
        COMMENT_DELETED
    }
    public enum ReportType {
        SPAM,
        HARASSMENT,
        INAPPROPRIATE_CONTENT,
        FAKE_ACCOUNT,
        OTHER
    }
    public enum ReportStatus {
        PENDING,
        RESOLVED,
        DISMISSED
    }
}
