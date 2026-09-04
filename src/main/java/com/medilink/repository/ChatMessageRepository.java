package com.medilink.repository;

import com.medilink.model.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT c FROM ChatMessage c WHERE (c.senderId = :u1 AND c.receiverId = :u2) " +
           "OR (c.senderId = :u2 AND c.receiverId = :u1) ORDER BY c.timestamp ASC")
    List<ChatMessage> findChatHistory(@Param("u1") String u1, @Param("u2") String u2);

    List<ChatMessage> findAllByOrderByTimestampAsc();
}
