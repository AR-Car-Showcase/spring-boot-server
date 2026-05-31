package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.dto.UserAccountDTO;
import com.arcarshowcaseserver.dto.UserPreferencesDTO;
import com.arcarshowcaseserver.dto.UserProfileDTO;
import com.arcarshowcaseserver.payload.response.MessageResponse;

public interface UserService {
    MessageResponse updateProfile(Long userId, UserAccountDTO profileDTO);
    MessageResponse updatePreferences(Long userId, UserPreferencesDTO preferencesDTO);
    UserProfileDTO getProfile(Long userId);
}
