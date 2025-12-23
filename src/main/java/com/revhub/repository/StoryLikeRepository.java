package com.revhub.repository;

import com.revhub.model.StoryLike;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryLikeRepository extends MongoRepository<StoryLike, String> {
    Optional<StoryLike> findByStoryIdAndUserId(String storyId, Long userId);

    void deleteByStoryId(String storyId);

    long countByStoryId(String storyId);
}
