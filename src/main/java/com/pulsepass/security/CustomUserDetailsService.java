package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Para el LOGIN. El identificador es username o email.
     * Incluye el hash porque DaoAuthenticationProvider lo necesita para comparar.
     * Un usuario legado sin credencial no puede iniciar sesión.
     */
    @Override
    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario sin credenciales"));

        return toPrincipal(user, credential.getPasswordHash());
    }

    /**
     * Para el filtro JWT (Paso 6). El token trae el id en "sub" y en cada request se
     * revalida estado y roles ACTUALES contra la base (SEC011). No lleva hash.
     */
    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return toPrincipal(user, null);
    }

    private Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        // Con '@' se trata como email; sin '@', como username.
        return value.contains("@")
                ? userRepository.findByEmailIgnoreCase(value)
                : userRepository.findByUsername(value);
    }

    private SecurityPrincipal toPrincipal(User user, String passwordHash) {
        List<GrantedAuthority> authorities = roleRepository.findByIdUserId(user.getId())
                .stream()
                .map(UserRole::getRole)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                passwordHash,
                user.isActive(),
                authorities);
    }
}