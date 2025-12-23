package com.revhub.repository;

import com.revhub.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
        List<Connection> findByRequesterIdAndStatus(Long requesterId, Connection.ConnectionStatus status);

        List<Connection> findByReceiverIdAndStatus(Long receiverId, Connection.ConnectionStatus status);

        @Query("SELECT c FROM Connection c WHERE " +
                        "(c.requesterId = :userId OR c.receiverId = :userId) AND " +
                        "c.status = :status")
        List<Connection> findByUserIdAndStatus(@Param("userId") Long userId,
                        @Param("status") Connection.ConnectionStatus status);

        @Query("SELECT c FROM Connection c WHERE " +
                        "((c.requesterId = :userId1 AND c.receiverId = :userId2) OR " +
                        "(c.requesterId = :userId2 AND c.receiverId = :userId1)) AND " +
                        "c.status != 'REJECTED'")
        Optional<Connection> findConnectionBetweenUsers(@Param("userId1") Long userId1,
                        @Param("userId2") Long userId2);

        @Query("SELECT c FROM Connection c WHERE " +
                        "(c.requesterId = :userId OR c.receiverId = :userId) AND " +
                        "c.status = 'ACCEPTED'")
        List<Connection> findAcceptedConnectionsByUserId(@Param("userId") Long userId);

        @Query("SELECT c FROM Connection c WHERE " +
                        "c.requesterId = :requesterId AND c.receiverId = :receiverId AND " +
                        "c.status != :status")
        Optional<Connection> findByRequesterIdAndReceiverIdAndStatusNot(
                        @Param("requesterId") Long requesterId,
                        @Param("receiverId") Long receiverId,
                        @Param("status") Connection.ConnectionStatus status);
}
