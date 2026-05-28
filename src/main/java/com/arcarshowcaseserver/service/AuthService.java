package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.enums.RoleType;
import com.arcarshowcaseserver.security.SecurityRolePermissionMapper;
import com.arcarshowcaseserver.model.Role;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.request.SignupRequest;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.repository.RoleRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;

    @Transactional
    public MessageResponse registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setProfilePic(signUpRequest.getProfilePic());

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleType.DEFAULT)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);

        user.setRoles(roles);
        user.setPermissions(SecurityRolePermissionMapper.permissionsForRoles(Set.of(RoleType.DEFAULT.name())));
        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }
}
