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

    @Query("SELECT c FROM ChatMessage c WHERE " +
           "((c.senderId IN (:ids1) OR (c.senderEmail IS NOT NULL AND c.senderEmail IN (:emails1))) AND " +
           " (c.receiverId IN (:ids2) OR (c.receiverEmail IS NOT NULL AND c.receiverEmail IN (:emails2)))) " +
           "OR " +
           "((c.senderId IN (:ids2) OR (c.senderEmail IS NOT NULL AND c.senderEmail IN (:emails2))) AND " +
           " (c.receiverId IN (:ids1) OR (c.receiverEmail IS NOT NULL AND c.receiverEmail IN (:emails1)))) " +
           "ORDER BY c.timestamp ASC")
    List<ChatMessage> findChatHistoryFlexible(
            @Param("ids1") List<String> ids1,
            @Param("emails1") List<String> emails1,
            @Param("ids2") List<String> ids2,
            @Param("emails2") List<String> emails2);

    List<ChatMessage> findAllByOrderByTimestampAsc();

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.receiverId = :receiverId AND (c.isRead = false OR c.isRead IS NULL)")
    long countUnreadByReceiverId(@Param("receiverId") String receiverId);

    @Query("SELECT c FROM ChatMessage c WHERE c.senderId IN (:ids) OR c.receiverId IN (:ids) " +
           "OR (c.senderEmail IS NOT NULL AND c.senderEmail IN (:emails)) " +
           "OR (c.receiverEmail IS NOT NULL AND c.receiverEmail IN (:emails)) " +
           "ORDER BY c.timestamp DESC")
    List<ChatMessage> findMessagesInvolvingUser(@Param("ids") List<String> ids, @Param("emails") List<String> emails);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE (c.receiverId IN (:ids) OR (c.receiverEmail IS NOT NULL AND c.receiverEmail IN (:emails))) AND (c.isRead = false OR c.isRead IS NULL)")
    long countUnreadFlexible(@Param("ids") List<String> ids, @Param("emails") List<String> emails);

    @Query("SELECT COUNT(c) FROM ChatMessage c WHERE (c.receiverId IN (:receiverIds) OR (c.receiverEmail IS NOT NULL AND c.receiverEmail IN (:receiverEmails))) " +
           "AND (c.senderId IN (:senderIds) OR (c.senderEmail IS NOT NULL AND c.senderEmail IN (:senderEmails))) " +
           "AND (c.isRead = false OR c.isRead IS NULL)")
    long countUnreadFromSenderFlexible(
            @Param("receiverIds") List<String> receiverIds,
            @Param("receiverEmails") List<String> receiverEmails,
            @Param("senderIds") List<String> senderIds,
            @Param("senderEmails") List<String> senderEmails);
}
