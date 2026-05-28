package com.arcarshowcaseserver.service.serviceImpl;

import com.arcarshowcaseserver.dto.UserPreferencesDTO;
import com.arcarshowcaseserver.dto.UserProfileDTO;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.payload.response.MessageResponse;
import com.arcarshowcaseserver.repository.CustomizationRepository;
import com.arcarshowcaseserver.repository.LikeRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CustomizationRepository customizationRepository;

    @Override
    @Transactional
    public MessageResponse updateProfile(Long userId, UserPreferencesDTO profileDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        if (profileDTO.getFavBrands() != null) {
            user.setFavBrands(new HashSet<>(profileDTO.getFavBrands()));
        }
        if (profileDTO.getPreferredBodyTypes() != null) {
            user.setPreferredBodyTypes(new HashSet<>(profileDTO.getPreferredBodyTypes()));
        }
        if (profileDTO.getPreferredFuelTypes() != null) {
            user.setPreferredFuelTypes(new HashSet<>(profileDTO.getPreferredFuelTypes()));
        }
        if (profileDTO.getPreferredTransmissions() != null) {
            user.setPreferredTransmissions(new HashSet<>(profileDTO.getPreferredTransmissions()));
        }
        if (profileDTO.getDrivingCondition() != null) user.setDrivingCondition(profileDTO.getDrivingCondition());
        if (profileDTO.getMaxBudget() != null) user.setMaxBudget(profileDTO.getMaxBudget());

        userRepository.save(user);
        return new MessageResponse("Profile updated successfully");
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
                user.getPhoneNumber(),
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
}
