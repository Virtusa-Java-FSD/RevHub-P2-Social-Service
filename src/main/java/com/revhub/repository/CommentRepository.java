package com.revhub.repository;

import com.revhub.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByPostIdOrderByCreatedAtDesc(String postId);

    Long countByPostId(String postId);

    void deleteByPostId(String postId);

    // Threaded comments support
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(String parentCommentId);

    Long countByParentCommentId(String parentCommentId);

    List<Comment> findByPostIdAndParentCommentIdIsNullOrderByCreatedAtDesc(String postId); // Top-level comments only
}
