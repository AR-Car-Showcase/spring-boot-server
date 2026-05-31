package com.arcarshowcaseserver.security;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.model.Role;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.RoleRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.sricharan.security.core.account.ExternalIdentityAccountLinker;
import com.sricharan.security.core.account.UserAccount;
import com.sricharan.security.core.account.UserAccountProvider;
import com.sricharan.security.core.identity.ExternalIdentityProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SecurityUserAccountProvider implements UserAccountProvider, ExternalIdentityAccountLinker {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityUserAccountProvider(UserRepository userRepository,
                                       RoleRepository roleRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        String candidate = username.trim();
        return userRepository.findByUsername(candidate)
                .or(() -> userRepository.findByEmailIgnoreCase(candidate))
                .or(() -> userRepository.findByEmail(candidate))
                .filter(this::isLoginEligible)
                .map(this::toUserAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByExternalIdentity(String provider, String subject) {
        if (!"google".equalsIgnoreCase(provider) || subject == null || subject.isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByExternalSubject(subject)
                .filter(this::isLoginEligible)
                .map(this::toUserAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .or(() -> userRepository.findByExternalEmail(normalizedEmail))
                .filter(this::isLoginEligible)
                .map(this::toUserAccount);
    }

    @Override
    @Transactional
    public UserAccount createOrLink(ExternalIdentityProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("External identity profile is required");
        }

        if (!"google".equalsIgnoreCase(profile.provider())) {
            throw new IllegalArgumentException("Unsupported external identity provider: " + profile.provider());
        }

        Optional<User> existingBySubject = userRepository.findByExternalSubject(profile.subject());
        if (existingBySubject.isPresent()) {
            User existing = existingBySubject.get();
            if (!isLoginEligible(existing)) {
                existing.setEmailVerified(true);
                existing.setExternalEmailVerified(true);
                existing.setAuthProvider("LINKED");
                existing.setExternalEmail(profile.email());
                return toUserAccount(userRepository.save(existing));
            }
            return toUserAccount(existing);
        }

        boolean hasExternalEmail = profile.email() != null && !profile.email().isBlank();
        if (hasExternalEmail && !profile.emailVerified()) {
            throw new IllegalArgumentException("Google email must be verified before account linking.");
        }

        Optional<User> existingByEmail = Optional.empty();
        if (hasExternalEmail) {
            String normalizedEmail = normalizeEmail(profile.email());
            existingByEmail = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .or(() -> userRepository.findByExternalEmail(normalizedEmail));
        }

        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            existing.setAuthProvider("LINKED");
            existing.setExternalSubject(profile.subject());
            existing.setExternalEmail(normalizeEmail(profile.email()));
            existing.setExternalEmailVerified(profile.emailVerified());
            existing.setEmailVerified(true);
            existing.setProfileCompleted(true);
            return toUserAccount(userRepository.save(existing));
        }

        User created = new User();
        created.setUsername(generateGoogleUsername(profile));
        created.setEmail(resolveEmail(profile));
        created.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        created.setAuthProvider("GOOGLE");
        created.setExternalSubject(profile.subject());
        created.setExternalEmail(profile.email());
        created.setExternalEmailVerified(profile.emailVerified());
        created.setEmailVerified(true);
        created.setProfileCompleted(false);

        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found"));
        created.setRoles(Set.of(userRole));
        created.setPermissions(SecurityRolePermissionMapper.permissionsForRoles(Set.of(RoleType.USER.name())));

        return toUserAccount(userRepository.save(created));
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

    private boolean isLoginEligible(User user) {
        if (user == null) {
            return false;
        }

        if ("LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
            return Boolean.TRUE.equals(user.getEmailVerified());
        }

        return true;
    }

    private String resolveEmail(ExternalIdentityProfile profile) {
        if (profile.email() != null && !profile.email().isBlank()) {
            return normalizeEmail(profile.email());
        }

        return generateGoogleUsername(profile) + "@google.local";
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateGoogleUsername(ExternalIdentityProfile profile) {
        String preferred = null;

        if (profile.email() != null && !profile.email().isBlank() && profile.email().contains("@")) {
            preferred = profile.email().substring(0, profile.email().indexOf('@'));
        } else if (profile.displayName() != null && !profile.displayName().isBlank()) {
            preferred = profile.displayName();
        }

        if (preferred != null) {
            String sanitized = preferred.toLowerCase()
                    .replaceAll("[^a-z0-9._]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^[._]+|[._]+$", "");

            if (!sanitized.isBlank()) {
                String base = sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
                String candidate = base;
                int suffix = 1;
                while (userRepository.existsByUsername(candidate)) {
                    String tail = "_" + suffix++;
                    int maxPrefixLength = Math.max(1, 20 - tail.length());
                    String prefix = base.length() > maxPrefixLength ? base.substring(0, maxPrefixLength) : base;
                    candidate = prefix + tail;
                }
                return candidate;
            }
        }

        return fallbackGoogleUsername(profile.subject());
    }

    private String fallbackGoogleUsername(String subject) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(subject.getBytes(StandardCharsets.UTF_8));
            return "google_" + HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException ex) {
            return "google_" + Integer.toHexString(subject.hashCode());
        }
    }
}
