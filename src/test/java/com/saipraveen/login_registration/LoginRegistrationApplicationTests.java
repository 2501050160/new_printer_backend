package com.saipraveen.login_registration;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import com.saipraveen.login_registration.config.CookieUtils;
import jakarta.servlet.http.Cookie;
import static org.junit.jupiter.api.Assertions.*;

class LoginRegistrationApplicationTests {

	@Test
	void testOAuth2AuthorizationRequestSerialization() {
		OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
				.clientId("test-client-id")
				.redirectUri("https://printer-backend-kgzp.onrender.com/login/oauth2/code/google")
				.state("test-state")
				.attributes(attrs -> attrs.put("registration_id", "google"))
				.build();

		String serialized = CookieUtils.serialize(request);
		assertNotNull(serialized);
		System.out.println("Serialized length: " + serialized.length());

		Cookie cookie = new Cookie("oauth2_auth_request", serialized);
		OAuth2AuthorizationRequest deserialized = CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class);
		assertNotNull(deserialized);
		assertEquals("test-client-id", deserialized.getClientId());
		assertEquals("test-state", deserialized.getState());
	}

}

