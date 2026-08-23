package com.saipraveen.login_registration.config;

import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.saipraveen.login_registration.entity.User;
import com.saipraveen.login_registration.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            response.sendRedirect(frontendUrl + "/login?error=OAuth2 email not found");
            return;
        }

        boolean isNewUser = false;
        User user = userRepository.findByEmail(email);
        if (user == null) {
            isNewUser = true;
            user = new User();
            user.setEmail(email);
            user.setName(name != null ? name : email.split("@")[0]);
            user.setPassword(UUID.randomUUID().toString()); // Random password
            user.setWalletBalance(0.0);
            user.setBlocked(false);
            user.setEmailVerified(true);
            
            // Generate referral code
            String code;
            do {
                code = String.valueOf(100000 + new java.util.Random().nextInt(900000));
            } while (userRepository.findByReferralCode(code) != null);
            user.setReferralCode(code);
            
            user = userRepository.save(user);
        }

        String redirectBase = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(jakarta.servlet.http.Cookie::getValue)
                .orElse(frontendUrl);

        if (redirectBase == null || redirectBase.isBlank() || redirectBase.equals("https://cloudprint.website")) {
            redirectBase = "https://www.cloudprint.website";
        }
        if (redirectBase.endsWith("/")) {
            redirectBase = redirectBase.substring(0, redirectBase.length() - 1);
        }

        String targetUrl = UriComponentsBuilder.fromUriString(redirectBase + "/login")
                .queryParam("oauth_success", "true")
                .queryParam("is_new_user", String.valueOf(isNewUser))
                .queryParam("id", user.getId())
                .queryParam("name", user.getName())
                .queryParam("email", user.getEmail())
                .queryParam("walletBalance", user.getWalletBalance())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
