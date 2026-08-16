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
            Supplier<? extends Authentication> authenticationSupplier,
            RequestAuthorizationContext authorizationContext
    ) {
        Authentication currentAuthentication = authenticationSupplier.get();
        if (currentAuthentication == null
                || !(currentAuthentication.getPrincipal() instanceof User authenticatedUser)) {
            return new AuthorizationDecision(false);
        }

        if (!authenticatedUser.isMustChangePassword()) {
            return new AuthorizationDecision(true);
        }

        HttpServletRequest httpServletRequest = authorizationContext.getRequest();
        String requestPath = httpServletRequest.getRequestURI()
                .substring(httpServletRequest.getContextPath().length());
        boolean allowed = requestPath.equals("/api/auth/me")
                || (requestPath.equals("/api/auth/password")
                && httpServletRequest.getMethod().equalsIgnoreCase("PATCH"));
        return new AuthorizationDecision(allowed);
    }
}
