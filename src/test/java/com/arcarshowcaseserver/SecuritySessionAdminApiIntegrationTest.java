package com.arcarshowcaseserver;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.model.Role;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.RoleRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.security.SecurityRolePermissionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecuritySessionAdminApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanListAndRevokeSessions() throws Exception {
        Role defaultRole = ensureRole(RoleType.DEFAULT);
        Role adminRole = ensureRole(RoleType.ADMIN);

        String userPassword = "Pass@123";
        User user = createUser("su_" + UUID.randomUUID().toString().substring(0, 8), userPassword, Set.of(defaultRole));
        User admin = createUser("sa_" + UUID.randomUUID().toString().substring(0, 8), userPassword, Set.of(defaultRole, adminRole));

        String userToken = login(user.getUsername(), userPassword);
        String adminToken = login(admin.getUsername(), userPassword);

        mockMvc.perform(get("/security/sessions")
                        .param("userId", String.valueOf(user.getId()))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        MvcResult listBefore = mockMvc.perform(get("/security/sessions")
                        .param("userId", String.valueOf(user.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sessionsBefore = objectMapper.readTree(listBefore.getResponse().getContentAsString());
        assertThat(sessionsBefore.isArray()).isTrue();
        assertThat(sessionsBefore.size()).isGreaterThanOrEqualTo(1);

        String sessionId = sessionsBefore.get(0).path("sessionId").asText();

        mockMvc.perform(delete("/security/sessions/{sessionId}", sessionId)
                        .param("userId", String.valueOf(user.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult listAfterSingle = mockMvc.perform(get("/security/sessions")
                        .param("userId", String.valueOf(user.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode sessionsAfterSingle = objectMapper.readTree(listAfterSingle.getResponse().getContentAsString());
        assertThat(sessionsAfterSingle.size()).isLessThan(sessionsBefore.size());

        mockMvc.perform(delete("/security/sessions/user/{userId}", user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    private Role ensureRole(RoleType roleType) {
        return roleRepository.findAllByName(roleType).stream().findFirst().orElseGet(() -> {
            Role role = new Role();
            role.setName(roleType);
            return roleRepository.save(role);
        });
    }

    private User createUser(String username, String rawPassword, Set<Role> roles) {
        User user = new User(username, username + "@example.com", passwordEncoder.encode(rawPassword));
        user.setAuthProvider("LOCAL");
        user.setEmailVerified(true);
        user.setRoles(roles);
        user.setPermissions(SecurityRolePermissionMapper.permissionsForRoles(
                roles.stream().map(role -> role.getName().name()).collect(java.util.stream.Collectors.toSet())
        ));
        return userRepository.save(user);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
