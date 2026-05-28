package com.arcarshowcaseserver.security;

import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.UserRepository;
import com.sricharan.security.core.account.UserAccount;
import com.sricharan.security.core.account.UserAccountProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SecurityUserAccountProvider implements UserAccountProvider {

    private final UserRepository userRepository;

    public SecurityUserAccountProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toUserAccount);
    }

    private UserAccount toUserAccount(User user) {
        Set<String> roles = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet());

        Set<String> permissions = SecurityRolePermissionMapper.mergeExplicitAndRolePermissions(
                roles,
                user.getPermissions()
        );

        return new UserAccount() {
            @Override
            public String getId() {
                return String.valueOf(user.getId());
            }

            @Override
            public String getUsername() {
                return user.getUsername();
            }

            @Override
            public String getPassword() {
                return user.getPassword();
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public Set<String> getPermissions() {
                return permissions;
            }
        };
    }
}
