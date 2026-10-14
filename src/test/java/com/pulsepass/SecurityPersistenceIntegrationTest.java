package com.pulsepass;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class SecurityPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired private UserRepository userRepository;
    @Autowired private UserCredentialRepository credentialRepository;
    @Autowired private UserRoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesV4() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history order by installed_rank",
                String.class);

        assertThat(versions).contains("1", "2", "3", "4");
    }

    @Test
    void persistsCredentialAndRoleForUser() {
        User user = userRepository.save(new User("andrea", "andrea@test.com"));

        credentialRepository.save(
                new UserCredential(user, "$2a$10$hash.ficticio.solo.para.pruebas"));
        roleRepository.save(new UserRole(user.getId(), Role.USER));

        assertThat(credentialRepository.findById(user.getId())).isPresent();
        assertThat(roleRepository.findByIdUserId(user.getId()))
                .extracting(UserRole::getRole)
                .containsExactly(Role.USER);
    }

    @Test
    void rejectsUnknownRole() {
        User user = userRepository.save(new User("carlos", "carlos@test.com"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into user_roles (user_id, role) values (?, 'SUPERUSER')",
                user.getId()))
                .isInstanceOf(DataAccessException.class);
    }
}