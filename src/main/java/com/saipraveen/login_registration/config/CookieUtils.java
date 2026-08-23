package com.saipraveen.login_registration.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);
        try {
            cookie.setAttribute("SameSite", "Lax");
        } catch (Exception ignored) {}
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    cookie.setSecure(true);
                    try {
                        cookie.setAttribute("SameSite", "Lax");
                    } catch (Exception ignored) {}
                    response.addCookie(cookie);
                }
            }
        }
    }

    public static String serialize(Object object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            return Base64.getUrlEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize object", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return (T) objectInputStream.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
