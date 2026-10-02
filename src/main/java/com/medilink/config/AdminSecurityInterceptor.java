package com.medilink.config;

import com.medilink.model.user.User;
import com.medilink.model.user.UserRole;
import com.medilink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

/**
 * Enterprise Security Interceptor for MediLink Admin Portal.
 * Restricts all /api/admin/** endpoints to authenticated Administrators only.
 * Rejects unauthenticated requests with 401 Unauthorized.
 * Rejects non-admin users (Patients, Pharmacists) with 403 Forbidden.
 */
@Component
public class AdminSecurityInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Autowired
    public AdminSecurityInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Allow CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. Extract identification from headers or session
        String userId = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");

        if (userId == null || userId.trim().isEmpty()) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("userId") != null) {
                userId = (String) session.getAttribute("userId");
            }
        }

        // 2. Reject unauthenticated access (401)
        if (userId == null || userId.trim().isEmpty()) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                    "Authentication required. Please log in as an administrator.");
            return false;
        }

        // 3. Verify user in database
        Optional<User> userOpt = userRepository.findById(userId.trim());
        if (!userOpt.isPresent()) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                    "Invalid administrative credentials or user not found.");
            return false;
        }

        User user = userOpt.get();

        // 4. Role Authorization: Reject Patients and Pharmacists (403 Forbidden)
        if (user.getRole() != UserRole.ADMIN || (roleHeader != null && !roleHeader.trim().isEmpty() && !"ADMIN".equalsIgnoreCase(roleHeader.trim()))) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                    "Access denied. Administrative role required.");
            return false;
        }

        // 5. Check if user is suspended or deactivated (403 Forbidden)
        String status = user.getStatus();
        if ("SUSPENDED".equalsIgnoreCase(status) || "DEACTIVATED".equalsIgnoreCase(status)) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_INACTIVE",
                    "Administrator account is " + status + ". Please contact system authority.");
            return false;
        }

        // Store authenticated context attributes
        request.setAttribute("authenticatedAdmin", user);
        request.setAttribute("adminId", user.getId());
        request.setAttribute("adminName", user.getName());

        return true;
    }

    private void sendJsonError(HttpServletResponse response, int statusCode, String code, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format("{\"status\":\"ERROR\",\"code\":\"%s\",\"message\":\"%s\"}", code, message);
        response.getWriter().write(json);
    }
}
