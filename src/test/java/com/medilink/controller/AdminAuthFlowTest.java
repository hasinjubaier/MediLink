package com.medilink.controller;

import com.medilink.model.user.Admin;
import com.medilink.model.user.User;
import com.medilink.model.user.UserRole;
import com.medilink.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminAuthFlowTest {

    @Mock
    private UserService userService;

    private AuthController authController;

    @BeforeEach
    public void setUp() {
        authController = new AuthController(userService);
    }

    @Test
    @DisplayName("Admin login with valid credentials should succeed and return administrative role")
    public void testAdminLoginSuccess() {
        String email = "admin@medilink.com";
        String password = "AdminPassword123!";

        Admin admin = new Admin("ADM_01", "Chief Admin", email, password, 2);
        admin.setStatus("ACTIVE");

        when(userService.findByEmail(email)).thenReturn(Optional.of(admin));
        when(userService.authenticate(email, password)).thenReturn(admin);

        Map<String, String> creds = new HashMap<>();
        creds.put("email", email);
        creds.put("password", password);

        ResponseEntity<Map<String, Object>> response = authController.login(creds);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.get("status"));
        assertEquals("ADM_01", body.get("id"));
        assertEquals("ADMIN", body.get("role"));
    }

    @Test
    @DisplayName("Login with non-existent email should return USER_NOT_FOUND error")
    public void testLoginUserNotFound() {
        String email = "ghost@medilink.com";
        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        Map<String, String> creds = new HashMap<>();
        creds.put("email", email);
        creds.put("password", "anyPass");

        ResponseEntity<Map<String, Object>> response = authController.login(creds);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.get("status"));
        assertEquals("USER_NOT_FOUND", body.get("code"));
    }

    @Test
    @DisplayName("Login with wrong password should return INVALID_PASSWORD error")
    public void testLoginInvalidPassword() {
        String email = "admin@medilink.com";
        User admin = new Admin("ADM_01", "Chief Admin", email, "correctPass", 2);

        when(userService.findByEmail(email)).thenReturn(Optional.of(admin));
        when(userService.authenticate(email, "wrongPass")).thenReturn(null);

        Map<String, String> creds = new HashMap<>();
        creds.put("email", email);
        creds.put("password", "wrongPass");

        ResponseEntity<Map<String, Object>> response = authController.login(creds);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.get("status"));
        assertEquals("INVALID_PASSWORD", body.get("code"));
    }

    @Test
    @DisplayName("Public self-registration of Administrator accounts must be rejected with 403 Forbidden")
    public void testSelfRegisterAdminForbidden() {
        Map<String, String> regData = new HashMap<>();
        regData.put("name", "Hacker");
        regData.put("email", "hacker@evil.com");
        regData.put("password", "pwned123");
        regData.put("role", "ADMIN");

        ResponseEntity<Map<String, Object>> response = authController.register(regData);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("ERROR", body.get("status"));
        assertTrue(body.get("message").toString().contains("Administrator accounts cannot be self-registered"));
    }
}
