package com.revhub.service;

import com.revhub.dto.PostRequest;
import com.revhub.exception.ResourceNotFoundException;
import com.revhub.exception.UnauthorizedException;
import com.revhub.model.*;
import com.revhub.repository.*;
import com.revhub.util.ContentParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final NotificationService notificationService;

    public Post createPost(Authentication authentication, PostRequest postRequest) {
        User user = userService.getCurrentUser(authentication);
        Post post = new Post();
        post.setContent(postRequest.getContent());
        // Handle media URLs (new) or legacy imageUrl
        if (postRequest.getMediaUrls() != null && !postRequest.getMediaUrls().isEmpty()) {
            post.setMediaUrls(postRequest.getMediaUrls());
        } else if (postRequest.getImageUrl() != null) {
            post.getMediaUrls().add(postRequest.getImageUrl());
        }
        // Set visibility (default to PUBLIC if not specified)
        post.setVisibility(postRequest.getVisibility() != null ? postRequest.getVisibility() : PostVisibility.PUBLIC);
        // Extract hashtags from content
        List<String> hashtags = ContentParser.extractHashtags(postRequest.getContent());
        post.setHashtags(hashtags);
        // Extract mentions and resolve to user IDs
        List<String> mentionUsernames = ContentParser.extractMentionUsernames(postRequest.getContent());
        List<Long> mentionedUserIds = new ArrayList<>();
        for (String username : mentionUsernames) {
            userRepository.findByUsername(username).ifPresent(u -> {
                mentionedUserIds.add(u.getId());
                // Create notification for mentioned user
                notificationService.createNotification(
                        Notification.NotificationType.MENTION,
                        u.getId(),
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName() + " mentioned you in a post",
                        null);
            });
        }
        post.setMentions(mentionedUserIds);
        post.setAuthorId(user.getId());
        post.setAuthorName(user.getFirstName() + " " + user.getLastName());
        post.setAuthorUsername(user.getUsername());
        post.setAuthorProfilePicture(user.getProfilePicture());
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    public List<Post> getFeed(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        // Get all accepted connections
        List<Connection> connections = connectionRepository.findAcceptedConnectionsByUserId(currentUser.getId());
        // Extract user IDs from connections
        List<Long> connectionIds = connections.stream()
                .map(conn -> conn.getRequesterId().equals(currentUser.getId())
                        ? conn.getReceiverId()
                        : conn.getRequesterId())
                .collect(Collectors.toList());
        // Add current user to see their own posts
        connectionIds.add(currentUser.getId());
        // Get all posts sorted by creation date
        List<Post> allPosts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        // Filter posts based on visibility
        List<Post> visiblePosts = allPosts.stream()
                .filter(post -> canUserViewPost(post, currentUser.getId(), connectionIds))
                .collect(Collectors.toList());

        // Populate isLiked status
        populateLikeStatus(visiblePosts, currentUser.getId());

        return visiblePosts;
    }

    private boolean canUserViewPost(Post post, Long userId, List<Long> userConnectionIds) {
        // User can always see their own posts
        if (post.getAuthorId().equals(userId)) {
            return true;
        }
        PostVisibility visibility = post.getVisibility() != null ? post.getVisibility() : PostVisibility.PUBLIC;
        switch (visibility) {
            case PUBLIC:
                return true;
            case CONNECTIONS_ONLY:
                return userConnectionIds.contains(post.getAuthorId());
            case PRIVATE:
                return false;
            default:
                return true;
        }
    }

    public List<Post> getUserPosts(Long userId, Authentication authentication) {
        List<Post> posts = postRepository.findByAuthorId(userId, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (authentication != null) {
            User currentUser = userService.getCurrentUser(authentication);
            populateLikeStatus(posts, currentUser.getId());
        }

        return posts;
    }

    // Helper method to populate isLiked field
    private void populateLikeStatus(List<Post> posts, Long currentUserId) {
        for (Post post : posts) {
            boolean isLiked = likeRepository.findByPostIdAndUserId(post.getId(), currentUserId).isPresent();
            post.setLiked(isLiked);
        }
    }

    public Post getPostById(String id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    public Post getPostById(String id, Authentication authentication) {
        Post post = getPostById(id);
        if (authentication != null) {
            User currentUser = userService.getCurrentUser(authentication);
            boolean isLiked = likeRepository.findByPostIdAndUserId(post.getId(), currentUser.getId()).isPresent();
            post.setLiked(isLiked);
        }
        return post;
    }

    @Transactional
    public Post updatePost(Authentication authentication, String postId, PostRequest postRequest) {
        User user = userService.getCurrentUser(authentication);
        Post post = getPostById(postId);
        if (!post.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only update your own posts");
        }
        if (postRequest.getContent() != null) {
            post.setContent(postRequest.getContent());
            // Re-extract hashtags and mentions if content changed
            List<String> hashtags = ContentParser.extractHashtags(postRequest.getContent());
            post.setHashtags(hashtags);
            List<String> mentionUsernames = ContentParser.extractMentionUsernames(postRequest.getContent());
            List<Long> mentionedUserIds = new ArrayList<>();
            for (String username : mentionUsernames) {
                userRepository.findByUsername(username).ifPresent(u -> mentionedUserIds.add(u.getId()));
            }
            post.setMentions(mentionedUserIds);
        }
        if (postRequest.getMediaUrls() != null) {
            post.setMediaUrls(postRequest.getMediaUrls());
        }
        if (postRequest.getVisibility() != null) {
            post.setVisibility(postRequest.getVisibility());
        }
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    @Transactional
    public void deletePost(Authentication authentication, String postId) {
        Post post = getPostById(postId);

        // If authentication is null, it's an admin delete - skip authorization
        if (authentication != null) {
            User user = userService.getCurrentUser(authentication);
            if (!post.getAuthorId().equals(user.getId())) {
                throw new UnauthorizedException("You can only delete your own posts");
            }
        }

        // Delete associated likes and comments
        likeRepository.deleteByPostId(postId);
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional
    public void likePost(Authentication authentication, String postId) {
        User user = userService.getCurrentUser(authentication);
        Post post = getPostById(postId);
        // Check if already liked
        if (likeRepository.findByPostIdAndUserId(postId, user.getId()).isPresent()) {
            return; // Already liked
        }
        Like like = new Like();
        like.setPostId(postId);
        like.setUserId(user.getId());
        like.setUserName(user.getFirstName() + " " + user.getLastName());
        like.setCreatedAt(LocalDateTime.now());
        likeRepository.save(like);
        // Update post likes count
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
        // Create notification for post author
        notificationService.createNotification(
                com.revhub.model.Notification.NotificationType.LIKE,
                post.getAuthorId(),
                user.getId(),
                user.getFirstName() + " " + user.getLastName() + " liked your post",
                postId);
    }

    @Transactional
    public void unlikePost(Authentication authentication, String postId) {
        User user = userService.getCurrentUser(authentication);
        Post post = getPostById(postId);
        Like like = likeRepository.findByPostIdAndUserId(postId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Like not found"));
        likeRepository.delete(like);
        // Update post likes count
        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);
        // Delete the like notification
        notificationService.deleteLikeNotification(post.getAuthorId(), user.getId(), postId);
    }

    @Transactional
    public void sharePost(String postId) {
        Post post = getPostById(postId);
        post.setSharesCount(post.getSharesCount() == null ? 1 : post.getSharesCount() + 1);
        postRepository.save(post);
    }

    public List<Comment> getComments(String postId) {
        getPostById(postId); // Verify post exists
        // Return only top-level comments (no parent)
        return commentRepository.findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(postId);
    }

    // Get replies to a specific comment
    public List<Comment> getReplies(String commentId) {
        // Verify comment exists
        commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        return commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
    }

    @Transactional
    public Comment addComment(Authentication authentication, String postId, String content) {
        return addComment(authentication, postId, content, null);
    }

    // Overloaded method to support threaded comments
    @Transactional
    public Comment addComment(Authentication authentication, String postId, String content, String parentCommentId) {
        User user = userService.getCurrentUser(authentication);
        Post post = getPostById(postId);

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setContent(content);
        comment.setAuthorId(user.getId());
        comment.setAuthorName(user.getFirstName() + " " + user.getLastName());
        comment.setAuthorProfilePicture(user.getProfilePicture());
        comment.setCreatedAt(LocalDateTime.now());

        // Handle threaded comments
        if (parentCommentId != null && !parentCommentId.isEmpty()) {
            Comment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found"));

            comment.setParentCommentId(parentCommentId);
            comment.setDepth(parentComment.getDepth() + 1);

            // Limit nesting depth to 5 levels
            if (comment.getDepth() > 5) {
                throw new com.revhub.exception.BadRequestException("Maximum comment nesting depth exceeded");
            }

            // Update parent comment's reply count
            parentComment.setRepliesCount(parentComment.getRepliesCount() + 1);
            commentRepository.save(parentComment);

            // Create notification for parent comment author (not post author)
            if (!parentComment.getAuthorId().equals(user.getId())) {
                notificationService.createNotification(
                        com.revhub.model.Notification.NotificationType.COMMENT,
                        parentComment.getAuthorId(),
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName() + " replied to your comment",
                        postId);
            }
        } else {
            comment.setParentCommentId(null);
            comment.setDepth(0);
            comment.setRepliesCount(0);

            // Create notification for post author
            if (!post.getAuthorId().equals(user.getId())) {
                notificationService.createNotification(
                        com.revhub.model.Notification.NotificationType.COMMENT,
                        post.getAuthorId(),
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName() + " commented on your post",
                        postId);
            }
        }

        Comment savedComment = commentRepository.save(comment);

        // Update post comments count (only for top-level comments)
        if (parentCommentId == null || parentCommentId.isEmpty()) {
            post.setCommentsCount(post.getCommentsCount() + 1);
            postRepository.save(post);
        }

        return savedComment;
    }

    @Transactional
    public Comment updateComment(Authentication authentication, String commentId, String content) {
        User user = userService.getCurrentUser(authentication);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only update your own comments");
        }
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Authentication authentication, String commentId) {
        User user = userService.getCurrentUser(authentication);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (!comment.getAuthorId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own comments");
        }

        // Delete all replies recursively
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        for (Comment reply : replies) {
            commentRepository.delete(reply);
        }

        // Update parent comment's reply count if this is a reply
        if (comment.getParentCommentId() != null) {
            commentRepository.findById(comment.getParentCommentId()).ifPresent(parentComment -> {
                parentComment.setRepliesCount(Math.max(0, parentComment.getRepliesCount() - 1));
                commentRepository.save(parentComment);
            });
        } else {
            // Update post comments count only for top-level comments
            Post post = getPostById(comment.getPostId());
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        }

        commentRepository.delete(comment);
    }

    // Hashtag methods (HP-1)
    public List<Post> getPostsByHashtag(String hashtag, Authentication authentication) {
        String cleanTag = hashtag.replace("#", "");
        // Exact match, case insensitive
        String regex = "^" + java.util.regex.Pattern.quote(cleanTag) + "$";
        List<Post> posts = postRepository.findByHashtagsRegex(regex, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (authentication != null) {
            User currentUser = userService.getCurrentUser(authentication);
            populateLikeStatus(posts, currentUser.getId());
        }
        return posts;
    }

    public Map<String, Long> getTrendingHashtags() {
        List<Post> allPosts = postRepository.findAll();
        // Count hashtag occurrences
        Map<String, Long> hashtagCounts = new HashMap<>();
        for (Post post : allPosts) {
            if (post.getHashtags() != null) {
                for (String hashtag : post.getHashtags()) {
                    hashtagCounts.put(hashtag, hashtagCounts.getOrDefault(hashtag, 0L) + 1);
                }
            }
        }
        // Return top 10 trending hashtags
        return hashtagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    // Search methods
    public List<Post> searchPosts(String keyword, Authentication authentication) {
        List<Post> posts = postRepository.searchByContent(keyword, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (authentication != null) {
            User currentUser = userService.getCurrentUser(authentication);
            populateLikeStatus(posts, currentUser.getId());
        }
        return posts;
    }

    // Mention methods (HP-2)
    public List<Post> getPostsMentioningUser(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        List<Post> allPosts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Post> posts = allPosts.stream()
                .filter(post -> post.getMentions() != null && post.getMentions().contains(user.getId()))
                .collect(Collectors.toList());
        populateLikeStatus(posts, user.getId());
        return posts;
    }

    // Admin method to delete post without authorization check (accepts String ID)
    @Transactional
    public void deletePostByAdminWithStringId(String postId) {
        Post post = getPostById(postId);
        // Delete associated likes and comments
        likeRepository.deleteByPostId(post.getId());
        commentRepository.deleteByPostId(post.getId());
        postRepository.delete(post);
    }

    // Admin method to delete post without authorization check (legacy Long ID
    // version)
    @Transactional
    public void deletePostByAdmin(Long postId, Authentication authentication) {
        Post post = getPostById(String.valueOf(postId));
        // Delete associated likes and comments
        likeRepository.deleteByPostId(post.getId());
        commentRepository.deleteByPostId(post.getId());
        postRepository.delete(post);
    }

    // Trending posts method
    public List<Post> getTrendingPosts(Authentication authentication) {
        // Get all public posts from the last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Post> allPosts = postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Filter recent posts and calculate trending score
        List<Post> trendingPosts = allPosts.stream()
                .filter(post -> post.getCreatedAt().isAfter(sevenDaysAgo))
                .filter(post -> post.getVisibility() == null || post.getVisibility() == PostVisibility.PUBLIC)
                .map(post -> {
                    // Calculate trending score
                    // Formula: (likes × 0.5) + (comments × 0.3) + (recency × 0.2)
                    double likesScore = post.getLikesCount() * 0.5;
                    double commentsScore = post.getCommentsCount() * 0.3;

                    // Recency score: newer posts get higher scores (0-100)
                    long hoursSinceCreation = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now())
                            .toHours();
                    double recencyScore = Math.max(0, (168 - hoursSinceCreation) / 168.0 * 100) * 0.2; // 168 hours = 7
                                                                                                       // days

                    double totalScore = likesScore + commentsScore + recencyScore;

                    // Store score in a temporary map (we'll sort by this)
                    return new Object[] { post, totalScore };
                })
                .sorted((a, b) -> Double.compare((Double) b[1], (Double) a[1])) // Sort by score descending
                .limit(20) // Return top 20 trending posts
                .map(arr -> (Post) arr[0])
                .collect(Collectors.toList());

        if (authentication != null) {
            User currentUser = userService.getCurrentUser(authentication);
            populateLikeStatus(trendingPosts, currentUser.getId());
        }

        return trendingPosts;
    }
}
