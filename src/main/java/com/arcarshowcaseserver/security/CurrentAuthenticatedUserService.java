package com.arcarshowcaseserver.security;

import com.sricharan.security.core.context.SecurityUserContext;
import com.sricharan.security.core.user.AuthenticatedUser;
import org.springframework.stereotype.Component;

@Component
public class CurrentAuthenticatedUserService {

    public AuthenticatedUser requireCurrentUser() {
        return SecurityUserContext.requireCurrentUser();
    }

    public Long requireCurrentUserIdAsLong() {
        AuthenticatedUser user = requireCurrentUser();
        try {
            return Long.parseLong(user.getUserId());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Authenticated user id is not a numeric value: " + user.getUserId(), ex);
        }
    }
}
