package com.pulsepass.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    protected UserRole() {
        // requerido por JPA
    }

    public UserRole(Long userId, Role role) {
        this.id = new UserRoleId(userId, role);
    }

    public UserRoleId getId() {
        return id;
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Role getRole() {
        return id.getRole();
    }
}