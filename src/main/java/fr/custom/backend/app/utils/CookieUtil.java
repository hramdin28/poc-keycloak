package fr.custom.backend.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.WebUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CookieUtil {

    public static Optional<Cookie> getCookie(
          HttpServletRequest request,
          String name) {

        return Optional.ofNullable(
              WebUtils.getCookie(request, name));
    }

    public static void addCookie(
          HttpServletResponse response,
          String name,
          String value,
          int maxAge,
          String path,
          boolean httpOnly) {

        Cookie cookie = new Cookie(name, value);
        cookie.setPath(path);
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(
          HttpServletRequest request,
          HttpServletResponse response,
          String name) {

        getCookie(request, name)
              .ifPresent(cookie -> {
                  cookie.setValue(StringUtils.EMPTY);
                  cookie.setMaxAge(0);
                  response.addCookie(cookie);
              });
    }

    public static String base64Encode(Object object) {
        try {
            if (object instanceof String stringObject) {
                return Base64.getEncoder().encodeToString(stringObject.getBytes());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            var json = objectMapper.writeValueAsString(object);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String base64Decode(String value) {
        var decodedValue = Base64.getDecoder().decode(value);
        return new String(decodedValue);
    }
}
