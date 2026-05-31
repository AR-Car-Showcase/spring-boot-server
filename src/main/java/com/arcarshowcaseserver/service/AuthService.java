package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.model.Role;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.request.EmailVerificationRequest;
import com.arcarshowcaseserver.payload.request.ResendVerificationRequest;
import com.arcarshowcaseserver.payload.request.SignupRequest;
import com.arcarshowcaseserver.payload.response.EmailVerificationResponse;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.repository.RoleRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.security.SecurityRolePermissionMapper;
import com.arcarshowcaseserver.service.verification.DisposableEmailPolicy;
import com.arcarshowcaseserver.service.verification.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final EmailVerificationService emailVerificationService;
    private final DisposableEmailPolicy disposableEmailPolicy;

    @Transactional
    public EmailVerificationResponse registerUser(SignupRequest signUpRequest) {
        String username = normalizeUsername(signUpRequest.getUsername());
        String email = normalizeEmail(signUpRequest.getEmail());

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken.");
        }

        if (disposableEmailPolicy.isDisposable(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Disposable or temporary email addresses are not allowed.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account with this email already exists. Please sign in or reset your password.");
        }

        User user = new User(username, email, encoder.encode(signUpRequest.getPassword()));
        user.setAuthProvider("LOCAL");
        user.setEmailVerified(false);
        user.setProfileCompleted(true);
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setProfilePic(signUpRequest.getProfilePic());

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleType.DEFAULT)
                .orElseThrow(() -> new IllegalStateException("DEFAULT role is not found."));
        roles.add(userRole);

        user.setRoles(roles);
        user.setPermissions(SecurityRolePermissionMapper.permissionsForRoles(Set.of(RoleType.DEFAULT.name())));
        User saved = userRepository.save(user);

        return emailVerificationService.issueVerificationCode(saved);
    }

    @Transactional
    public MessageResponse verifyEmail(EmailVerificationRequest request) {
        return emailVerificationService.verifyEmail(request.getEmail(), request.getCode());
    }

    @Transactional
    public EmailVerificationResponse resendVerification(ResendVerificationRequest request) {
        return emailVerificationService.resendVerification(request.getEmail());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }
}
