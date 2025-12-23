package com.revhub.repository;

import com.revhub.model.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByAuthorId(Long authorId, Sort sort);

    List<Post> findByAuthorIdIn(List<Long> authorIds, Sort sort);

    List<Post> findAllByOrderByCreatedAtDesc();

    // Hashtag search (HP-1)
    // Hashtag search (HP-1)
    @Query("{ 'hashtags': { $regex: ?0, $options: 'i' } }")
    List<Post> findByHashtagsRegex(String regex, Sort sort);

    // Search posts by content
    @Query("{ 'content': { $regex: ?0, $options: 'i' } }")
    List<Post> searchByContent(String keyword, Sort sort);

    // Find posts where user is mentioned
    List<Post> findByMentionsContaining(Long userId, Sort sort);

    // Admin queries
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    long countByCreatedAtAfter(LocalDateTime date);

    List<Post> findByContentContainingIgnoreCase(String query);
}
