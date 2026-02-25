package com.coworking_hub.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_USER_ATTR = "authenticatedUser";

    private final JwtService jwtService;

    public JwtAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Optional<String> tokenOptional = jwtService.resolveTokenFromAuthorizationHeader(request.getHeader("Authorization"));

        if (tokenOptional.isEmpty()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Nedostaje ili je neispravan Authorization header.");
            return false;
        }

        String token = tokenOptional.get();
        if (!jwtService.isTokenValid(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token nije validan ili je istekao.");
            return false;
        }

        String role = jwtService.extractRole(token);
        String status = jwtService.extractStatus(token);

        if (status == null || !"odobren".equalsIgnoreCase(status)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Korisnik nije odobren.");
            return false;
        }

        String requestPath = request.getRequestURI();
        if (!hasRequiredRole(requestPath, role)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Nemate dozvolu za pristup ovoj ruti.");
            return false;
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                jwtService.extractUserId(token),
                jwtService.extractUsername(token),
                role,
                status
        );

        request.setAttribute(AUTH_USER_ATTR, authenticatedUser);
        return true;
    }

    private boolean hasRequiredRole(String requestPath, String role) {
        if (requestPath.startsWith("/api/admin/")) {
            return "admin".equalsIgnoreCase(role);
        }
        if (requestPath.startsWith("/api/manager/")) {
            return "menadzer".equalsIgnoreCase(role);
        }
        if (requestPath.startsWith("/api/member/")) {
            return "clan".equalsIgnoreCase(role);
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
