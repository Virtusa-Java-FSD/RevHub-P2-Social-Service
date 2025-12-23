package com.revhub.dto;
import com.revhub.model.PostVisibility;
import lombok.Data;
import java.util.List;
@Data
public class PostRequest {
    private String content;
    @Deprecated // Use mediaUrls instead
    private String imageUrl;
    private List<String> mediaUrls;
    private PostVisibility visibility;
}
