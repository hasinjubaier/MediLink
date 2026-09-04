package com.medilink.model.user;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Base Class for all users in the MediLink system.
 * Demonstrates Abstraction, Encapsulation, and Polymorphic JPA Inheritance (JOINED).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @Column(name = "id", length = 50)
    protected String id;

    @Column(name = "name", nullable = false, length = 100)
    protected String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    protected String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    protected String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    protected UserRole role = UserRole.PATIENT;

    @Column(name = "phone", length = 50)
    protected String phone;

    @Column(name = "avatar_emoji", length = 10)
    protected String avatarEmoji = "👤";

    @Column(name = "custom_avatar", columnDefinition = "TEXT")
    protected String customAvatar;

    @Column(name = "created_at")
    protected LocalDateTime createdAt;

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String id, String name, String email, String passwordHash, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.PATIENT;
        this.createdAt = LocalDateTime.now();
    }

    public User(String id, String name, String email, String passwordHash, UserRole role, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : UserRole.PATIENT;
        this.phone = phone;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarEmoji() { return avatarEmoji; }
    public void setAvatarEmoji(String avatarEmoji) { this.avatarEmoji = avatarEmoji; }

    public String getCustomAvatar() { return customAvatar; }
    public void setCustomAvatar(String customAvatar) { this.customAvatar = customAvatar; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Polymorphic method implementations
    public String getDashboardInfo() {
        return "MediLink Portal: Access healthcare services, prescriptions, and verified pharmacy stock.";
    }

    public Set<String> getPermissions() {
        return new HashSet<>(Arrays.asList("SEARCH_MEDICINE", "LOCATE_PHARMACY"));
    }
}
