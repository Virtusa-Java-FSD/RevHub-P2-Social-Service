package com.revhub.repository;
import com.revhub.model.Story;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    List<Story> findByAuthorId(Long authorId);
    List<Story> findByAuthorIdIn(List<Long> authorIds);
    @Query("{ 'expiresAt': { $gt: ?0 } }")
    List<Story> findActiveStories(LocalDateTime now);
    @Query("{ 'authorId': { $in: ?0 }, 'expiresAt': { $gt: ?1 } }")
    List<Story> findActiveStoriesByAuthorIds(List<Long> authorIds, LocalDateTime now);
}
