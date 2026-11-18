package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.UserRoleId;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleService {
    private final UserRepository users;
    private final UserRoleRepository roles;

    public AdminRoleService(UserRepository users, UserRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    public record AccountRoles(Long id, String username, String email, List<Role> roles) {}

    @Transactional(readOnly = true)
    public AccountRoles find(String identifier) {
        User user = users.findByUsername(identifier.trim())
                .or(() -> users.findByEmailIgnoreCase(identifier.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return response(user);
    }

    @Transactional
    public AccountRoles add(Long userId, Role role) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Agregar un rol conserva los permisos que la cuenta ya tenia.
        if (!roles.existsById(new UserRoleId(userId, role))) {
            roles.save(new UserRole(userId, role));
        }
        return response(user);
    }

    private AccountRoles response(User user) {
        return new AccountRoles(user.getId(), user.getUsername(), user.getEmail(),
                roles.findByIdUserId(user.getId()).stream().map(UserRole::getRole).toList());
    }
}
