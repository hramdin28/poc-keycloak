package fr.custom.backend.app.expositions.controllers;

import fr.custom.backend.app.security.AuthenticationComponent;
import fr.custom.backend.app.security.model.AuthenticationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/auth/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthenticationComponent authenticationComponent;

    @GetMapping("/isLoggedIn")
    public boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }


    @GetMapping("/oauth-token")
    public AuthenticationResponse getOauthTokens(
          @RequestParam String registrationId,
          @RequestParam String code,
          @RequestParam(required = false) String codeVerifier,
          HttpServletResponse response) {

        return authenticationComponent.getOauthTokens(registrationId, code, codeVerifier, response);
    }

    @GetMapping("/refresh-token")
    public AuthenticationResponse refreshToken(
          @RequestParam String registrationId,
          HttpServletRequest request,
          HttpServletResponse response) {

        return authenticationComponent.refreshTokens(registrationId, request, response);
    }

    @ApiResponse(responseCode = "200", description = "Logout token")
    @PostMapping(path = "logout")
    public void logout(
          @RequestParam String registrationId,
          HttpServletRequest request,
          HttpServletResponse response) {
        authenticationComponent.logout(request, response, registrationId);
    }
}
