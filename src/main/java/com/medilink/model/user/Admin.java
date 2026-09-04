package com.medilink.model.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete class representing an Administrator user.
 * Demonstrates Inheritance, Polymorphism, and JPA JOINED subclass mapping.
 */
@Entity
@Table(name = "admins")
public class Admin extends User {

    @Column(name = "access_level")
    private int accessLevel;

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    public Admin(String id, String name, String email, String passwordHash, int accessLevel) {
        super(id, name, email, passwordHash, UserRole.ADMIN);
        this.accessLevel = accessLevel;
    }

    public int getAccessLevel() { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

    @Override
    public String getDashboardInfo() {
        return "Admin Dashboard: System Telemetry, Medicine Database Management, Pharmacy Auditing, Security Logs.";
    }

    @Override
    public Set<String> getPermissions() {
        return new HashSet<>(Arrays.asList(
            "MANAGE_USERS",
            "MANAGE_MEDICINES",
            "MANAGE_PHARMACIES",
            "AUDIT_LOGS",
            "SYSTEM_CONFIG"
        ));
    }
}
