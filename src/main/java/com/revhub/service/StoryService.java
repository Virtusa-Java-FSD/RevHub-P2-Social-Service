package com.revhub.service;

import com.revhub.dto.StoryRequest;
import com.revhub.exception.ResourceNotFoundException;
import com.revhub.exception.UnauthorizedException;
import com.revhub.model.Connection;
import com.revhub.model.Story;
import com.revhub.model.StoryLike;
import com.revhub.model.User;
import com.revhub.repository.ConnectionRepository;
import com.revhub.repository.StoryLikeRepository;
import com.revhub.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {
    private final StoryRepository storyRepository;
    private final UserService userService;
    private final ConnectionRepository connectionRepository;
    private final StoryLikeRepository storyLikeRepository;
    private final NotificationService notificationService;

    public Story createStory(Authentication authentication, StoryRequest storyRequest) {
        User user = userService.getCurrentUser(authentication);
        Story story = new Story();
        story.setImageUrl(storyRequest.getImageUrl());
        story.setMediaType(storyRequest.getMediaType());
        story.setCaption(storyRequest.getCaption());
        story.setAuthorId(user.getId());
        story.setAuthorName(user.getFirstName() + " " + user.getLastName());
        story.setAuthorProfilePicture(user.getProfilePicture());
        story.setCreatedAt(LocalDateTime.now());
        story.setExpiresAt(LocalDateTime.now().plusHours(24));
        return storyRepository.save(story);
    }

    public List<Story> getActiveStories(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        // Get all accepted connections
        List<Connection> connections = connectionRepository.findAcceptedConnectionsByUserId(currentUser.getId());
        // Extract user IDs from connections
        List<Long> connectionIds = connections.stream()
                .map(conn -> conn.getRequesterId().equals(currentUser.getId())
                        ? conn.getReceiverId()
                        : conn.getRequesterId())
                .collect(Collectors.toList());
        // Add current user to see their own stories
        connectionIds.add(currentUser.getId());
        // Get active stories from connections
        return storyRepository.findActiveStoriesByAuthorIds(connectionIds, LocalDateTime.now());
    }

    public Story getStoryById(String id) {
        return storyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found with id: " + id));
    }

    @Transactional
    public void deleteStory(Authentication authentication, String storyId) {
        User user = userService.getCurrentUser(authentication);
        Story story = getStoryById(storyId);
        if (!story.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own stories");
        }
        // Delete associated likes
        storyLikeRepository.deleteByStoryId(storyId);
        storyRepository.delete(story);
    }

    @Transactional
    public void likeStory(Authentication authentication, String storyId) {
        User user = userService.getCurrentUser(authentication);
        Story story = getStoryById(storyId);

        // Check if already liked
        if (storyLikeRepository.findByStoryIdAndUserId(storyId, user.getId()).isPresent()) {
            return; // Already liked
        }

        StoryLike like = new StoryLike();
        like.setStoryId(storyId);
        like.setUserId(user.getId());
        like.setUserName(user.getFirstName() + " " + user.getLastName());
        like.setCreatedAt(LocalDateTime.now());
        storyLikeRepository.save(like);

        // Update story likes count
        story.setLikesCount(story.getLikesCount() + 1);
        storyRepository.save(story);

        // Create notification for story author (don't notify yourself)
        if (!story.getAuthorId().equals(user.getId())) {
            notificationService.createNotification(
                    com.revhub.model.Notification.NotificationType.STORY_LIKE,
                    story.getAuthorId(),
                    user.getId(),
                    user.getFirstName() + " " + user.getLastName() + " liked your story",
                    storyId);
        }
    }

    @Transactional
    public void unlikeStory(Authentication authentication, String storyId) {
        User user = userService.getCurrentUser(authentication);
        Story story = getStoryById(storyId);

        StoryLike like = storyLikeRepository.findByStoryIdAndUserId(storyId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));

        storyLikeRepository.delete(like);

        // Update story likes count
        story.setLikesCount(Math.max(0, story.getLikesCount() - 1));
        storyRepository.save(story);

        // Delete the like notification
        notificationService.deleteLikeNotification(story.getAuthorId(), user.getId(), storyId);
    }
}
