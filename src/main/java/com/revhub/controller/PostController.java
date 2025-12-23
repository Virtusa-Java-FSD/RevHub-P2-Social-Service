package com.revhub.controller;

import com.revhub.dto.CommentRequest;
import com.revhub.dto.MessageResponse;
import com.revhub.dto.PostRequest;
import com.revhub.model.Comment;
import com.revhub.model.Post;
import com.revhub.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<Post> createPost(Authentication authentication, @RequestBody PostRequest postRequest) {
        Post post = postService.createPost(authentication, postRequest);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<Post>> getFeed(Authentication authentication) {
        List<Post> posts = postService.getFeed(authentication);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable Long userId, Authentication authentication) {
        List<Post> posts = postService.getUserPosts(userId, authentication);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable String id, Authentication authentication) {
        Post post = postService.getPostById(id, authentication);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(Authentication authentication,
            @PathVariable String id,
            @RequestBody PostRequest postRequest) {
        Post post = postService.updatePost(authentication, id, postRequest);
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePost(Authentication authentication, @PathVariable String id) {
        postService.deletePost(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Post deleted successfully"));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<MessageResponse> likePost(Authentication authentication, @PathVariable String id) {
        postService.likePost(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Post liked successfully"));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<MessageResponse> unlikePost(Authentication authentication, @PathVariable String id) {
        postService.unlikePost(authentication, id);
        return ResponseEntity.ok(new MessageResponse("Post unliked successfully"));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<MessageResponse> sharePost(@PathVariable String id) {
        postService.sharePost(id);
        return ResponseEntity.ok(new MessageResponse("Post shared successfully"));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable String id) {
        List<Comment> comments = postService.getComments(id);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(Authentication authentication,
            @PathVariable String id,
            @RequestBody CommentRequest commentRequest) {
        Comment comment = postService.addComment(authentication, id, commentRequest.getContent(),
                commentRequest.getParentCommentId());
        return ResponseEntity.ok(comment);
    }

    // Get replies to a specific comment
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable String commentId) {
        List<Comment> replies = postService.getReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<Comment> updateComment(Authentication authentication,
            @PathVariable String commentId,
            @RequestBody CommentRequest commentRequest) {
        Comment comment = postService.updateComment(authentication, commentId, commentRequest.getContent());
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<MessageResponse> deleteComment(Authentication authentication,
            @PathVariable String commentId) {
        postService.deleteComment(authentication, commentId);
        return ResponseEntity.ok(new MessageResponse("Comment deleted successfully"));
    }

    // Hashtag endpoints (HP-1)
    @GetMapping("/hashtag/{tag}")
    public ResponseEntity<List<Post>> getPostsByHashtag(@PathVariable String tag, Authentication authentication) {
        List<Post> posts = postService.getPostsByHashtag(tag, authentication);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/hashtags/trending")
    public ResponseEntity<Map<String, Object>> getTrendingHashtags() {
        Map<String, Long> trending = postService.getTrendingHashtags();
        Map<String, Object> response = new HashMap<>();
        response.put("hashtags", trending);
        return ResponseEntity.ok(response);
    }

    // Search endpoints
    @GetMapping("/search")
    public ResponseEntity<List<Post>> searchPosts(@RequestParam String q, Authentication authentication) {
        List<Post> posts = postService.searchPosts(q, authentication);
        return ResponseEntity.ok(posts);
    }

    // Mention endpoints (HP-2)
    @GetMapping("/mentions/me")
    public ResponseEntity<List<Post>> getPostsMentioningMe(Authentication authentication) {
        List<Post> posts = postService.getPostsMentioningUser(authentication);
        return ResponseEntity.ok(posts);
    }

    // Trending posts endpoint
    @GetMapping("/trending")
    public ResponseEntity<List<Post>> getTrendingPosts(Authentication authentication) {
        List<Post> posts = postService.getTrendingPosts(authentication);
        return ResponseEntity.ok(posts);
    }
}
