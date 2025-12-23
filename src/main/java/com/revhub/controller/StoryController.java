package com.revhub.controller;

import com.revhub.dto.MessageResponse;
import com.revhub.dto.StoryRequest;
import com.revhub.model.Story;
import com.revhub.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {
    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<Story> createStory(Authentication authentication, @RequestBody StoryRequest storyRequest) {
        Story story = storyService.createStory(authentication, storyRequest);
        return ResponseEntity.ok(story);
    }

    @GetMapping
    public ResponseEntity<List<Story>> getActiveStories(Authentication authentication) {
        List<Story> stories = storyService.getActiveStories(authentication);
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Story> getStoryById(@PathVariable String id) {
        Story story = storyService.getStoryById(id);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteStory(Authentication authentication, @PathVariable String id) {
        storyService.deleteStory(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Story deleted successfully"));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<MessageResponse> likeStory(Authentication authentication, @PathVariable String id) {
        storyService.likeStory(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Story liked successfully"));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<MessageResponse> unlikeStory(Authentication authentication, @PathVariable String id) {
        storyService.unlikeStory(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Story unliked successfully"));
    }
}
