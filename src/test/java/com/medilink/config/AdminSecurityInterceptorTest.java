package com.medilink.config;

import com.medilink.model.user.Admin;
import com.medilink.model.user.Patient;
import com.medilink.model.user.Pharmacist;
import com.medilink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminSecurityInterceptorTest {

    @Mock
    private UserRepository userRepository;

    private AdminSecurityInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = new AdminSecurityInterceptor(userRepository);
    }

    @Test
    @DisplayName("CORS preflight OPTIONS requests should bypass authentication")
    public void testCorsPreflightOptionsRequestPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertTrue(allowed);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("Unauthenticated request with no headers should return 401 Unauthorized")
    public void testUnauthenticatedNoHeadersReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/admin/telemetry");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Request with non-existent user ID should return 401 Unauthorized")
    public void testUserNotFoundReturns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "NONEXISTENT_USER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.findById("NONEXISTENT_USER")).thenReturn(Optional.empty());

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid administrative credentials"));
    }

    @Test
    @DisplayName("Patient accessing Admin API should be rejected with 403 Forbidden")
    public void testPatientRoleForbiddenReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "PATIENT_01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Patient patient = new Patient("P_01", "Rahim", "rahim@patient.com", "pass", "01711111111", "Dhaka", "01711111111");
        when(userRepository.findById("PATIENT_01")).thenReturn(Optional.of(patient));

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("Access denied"));
    }

    @Test
    @DisplayName("Pharmacist accessing Admin API should be rejected with 403 Forbidden")
    public void testPharmacistRoleForbiddenReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "PHARM_01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Pharmacist pharmacist = new Pharmacist("PH_01", "Karim", "karim@pharm.com", "pass", "ph_01", "Lazz Pharma", "LIC-1234");
        when(userRepository.findById("PHARM_01")).thenReturn(Optional.of(pharmacist));

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("Access denied"));
    }

    @Test
    @DisplayName("Suspended Administrator accessing Admin API should be rejected with 403 Forbidden")
    public void testSuspendedAdminForbiddenReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "ADMIN_SUSPENDED");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Admin suspendedAdmin = new Admin("A_SUS", "Bad Admin", "bad@medilink.com", "pass", 1);
        suspendedAdmin.setStatus("SUSPENDED");
        when(userRepository.findById("ADMIN_SUSPENDED")).thenReturn(Optional.of(suspendedAdmin));

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("Deactivated Administrator accessing Admin API should be rejected with 403 Forbidden")
    public void testDeactivatedAdminForbiddenReturns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "ADMIN_DEACT");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Admin deactAdmin = new Admin("A_DEACT", "Old Admin", "old@medilink.com", "pass", 1);
        deactAdmin.setStatus("DEACTIVATED");
        when(userRepository.findById("ADMIN_DEACT")).thenReturn(Optional.of(deactAdmin));

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("Active Administrator accessing Admin API should pass and populate request context")
    public void testActiveAdminSuccessReturnsTrueAndSetsAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.addHeader("X-User-Id", "ADMIN_VALID");
        request.addHeader("X-User-Role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Admin validAdmin = new Admin("ADM_01", "Super Admin", "admin@medilink.com", "pass", 2);
        validAdmin.setStatus("ACTIVE");
        when(userRepository.findById("ADMIN_VALID")).thenReturn(Optional.of(validAdmin));

        boolean allowed = interceptor.preHandle(request, response, new Object());
        assertTrue(allowed);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("ADM_01", request.getAttribute("adminId"));
        assertEquals("Super Admin", request.getAttribute("adminName"));
        assertEquals(validAdmin, request.getAttribute("authenticatedAdmin"));
    }
}
