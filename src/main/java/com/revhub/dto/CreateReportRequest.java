package com.revhub.dto;
import com.revhub.model.Report;
import lombok.Data;
@Data
public class CreateReportRequest {
    private String reportedEntityId;
    private String reportedEntityType; // "USER" or "POST"
    private Report.ReportType type;
    private String description;
}
