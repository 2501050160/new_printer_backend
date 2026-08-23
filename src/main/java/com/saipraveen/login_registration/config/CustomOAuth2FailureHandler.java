package com.saipraveen.login_registration.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2FailureHandler.class);

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @Value("${app.frontend.url:https://cloudprint.website}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        log.error("Google OAuth2 Authentication Failed: {}", exception.getMessage(), exception);
        
        String errorMessage = "Google sign-in failed. Please try again.";
        if (exception.getMessage() != null) {
            String msg = exception.getMessage().toLowerCase();
            if (msg.contains("invalid_token_response") || msg.contains("invalid_client")) {
                errorMessage = "Google authentication rejected (Invalid client secret or token response). Please check Google Cloud credentials.";
            } else if (msg.contains("authorization_request_not_found")) {
                errorMessage = "Authentication session timed out or cookies are blocked. Please try again.";
            } else if (msg.contains("access_denied")) {
                errorMessage = "Google login was cancelled or access was denied.";
            } else {
                errorMessage = "Google login error: " + exception.getMessage();
            }
        }

        String targetUrl = frontendUrl + "/login?error=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
