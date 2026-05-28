package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.dto.UserPreferencesDTO;
import com.arcarshowcaseserver.dto.UserProfileDTO;
import com.arcarshowcaseserver.payload.response.MessageResponse;

public interface UserService {
    MessageResponse updateProfile(Long userId, UserPreferencesDTO profileDTO);
    UserProfileDTO getProfile(Long userId);
}
