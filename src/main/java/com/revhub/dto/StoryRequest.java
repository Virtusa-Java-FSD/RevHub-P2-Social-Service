package com.revhub.dto;
import lombok.Data;
@Data
public class StoryRequest {
    private String imageUrl;
    private String mediaType; // "text", "image", or "video"
    private String caption;
}
