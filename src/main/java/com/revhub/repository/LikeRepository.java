package com.revhub.repository;
import com.revhub.model.Like;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface LikeRepository extends MongoRepository<Like, String> {
    Optional<Like> findByPostIdAndUserId(String postId, Long userId);
    List<Like> findByPostId(String postId);
    Long countByPostId(String postId);
    void deleteByPostIdAndUserId(String postId, Long userId);
    void deleteByPostId(String postId);
}
