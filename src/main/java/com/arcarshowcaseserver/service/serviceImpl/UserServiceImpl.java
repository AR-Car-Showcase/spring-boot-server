package com.arcarshowcaseserver.service.serviceImpl;

import com.arcarshowcaseserver.dto.UserAccountDTO;
import com.arcarshowcaseserver.dto.UserPreferencesDTO;
import com.arcarshowcaseserver.dto.UserProfileDTO;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.repository.CustomizationRepository;
import com.arcarshowcaseserver.repository.LikeRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.service.UserService;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CustomizationRepository customizationRepository;

    @Override
    @Transactional
    public MessageResponse updateProfile(Long userId, UserAccountDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        String requestedUsername = normalize(profileDTO.getUsername());
        if (requestedUsername != null && !requestedUsername.isBlank()) {
            boolean usernameChanged = !requestedUsername.equalsIgnoreCase(user.getUsername());
            if (Boolean.TRUE.equals(user.getProfileCompleted()) && usernameChanged) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Username cannot be changed after account setup.");
            }

            if (usernameChanged && userRepository.existsByUsername(requestedUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken.");
            }
            user.setUsername(requestedUsername);
            user.setProfileCompleted(true);
        }

        if (profileDTO.getDisplayName() != null) {
            user.setDisplayName(blankToNull(profileDTO.getDisplayName()));
        }

        if (profileDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(blankToNull(profileDTO.getPhoneNumber()));
        }

        if (profileDTO.getBio() != null) {
            user.setBio(blankToNull(profileDTO.getBio()));
        }

        if (profileDTO.getProfilePic() != null) {
            user.setProfilePic(blankToNull(profileDTO.getProfilePic()));
        }

        userRepository.save(user);
        return new MessageResponse("Profile updated successfully");
    }

    @Override
    @Transactional
    public MessageResponse updatePreferences(Long userId, UserPreferencesDTO preferencesDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        if (preferencesDTO.getFavBrands() != null) {
            user.setFavBrands(new HashSet<>(preferencesDTO.getFavBrands()));
        }
        if (preferencesDTO.getPreferredBodyTypes() != null) {
            user.setPreferredBodyTypes(new HashSet<>(preferencesDTO.getPreferredBodyTypes()));
        }
        if (preferencesDTO.getPreferredFuelTypes() != null) {
            user.setPreferredFuelTypes(new HashSet<>(preferencesDTO.getPreferredFuelTypes()));
        }
        if (preferencesDTO.getPreferredTransmissions() != null) {
            user.setPreferredTransmissions(new HashSet<>(preferencesDTO.getPreferredTransmissions()));
        }
        if (preferencesDTO.getDrivingCondition() != null) user.setDrivingCondition(preferencesDTO.getDrivingCondition());
        if (preferencesDTO.getMaxBudget() != null) user.setMaxBudget(preferencesDTO.getMaxBudget());

        userRepository.save(user);
        return new MessageResponse("Preferences updated successfully");
    }

    @Override
    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        long savedCount = likeRepository.countByUser(user);
        long customizedCount = customizationRepository.countByUser(user);

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAuthProvider(),
                user.getProfileCompleted(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getBio(),
                user.getProfilePic(),
                user.getFavBrands(),
                user.getPreferredBodyTypes(),
                user.getPreferredFuelTypes(),
                user.getPreferredTransmissions(),
                user.getDrivingCondition(),
                user.getMaxBudget(),
                savedCount,
                customizedCount
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
