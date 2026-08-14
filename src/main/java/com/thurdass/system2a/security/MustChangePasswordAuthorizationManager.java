package com.thurdass.system2a.security;

import com.thurdass.system2a.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MustChangePasswordAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context
    ) {
        Authentication currentAuthentication = authentication.get();
        if (currentAuthentication == null
                || !(currentAuthentication.getPrincipal() instanceof User user)) {
            return new AuthorizationDecision(false);
        }

        if (!user.isMustChangePassword()) {
            return new AuthorizationDecision(true);
        }

        HttpServletRequest request = context.getRequest();
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        boolean allowed = requestPath.equals("/api/auth/me")
                || (requestPath.equals("/api/auth/password")
                && request.getMethod().equalsIgnoreCase("PATCH"));
        return new AuthorizationDecision(allowed);
    }
}
