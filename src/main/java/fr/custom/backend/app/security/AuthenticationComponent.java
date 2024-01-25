package fr.custom.backend.app.security;

import com.fasterxml.jackson.databind.JsonNode;
import fr.custom.backend.app.properties.OauthProperties;
import fr.custom.backend.app.security.constants.AuthConstants;
import fr.custom.backend.app.security.model.AuthenticationResponse;
import fr.custom.backend.app.utils.CookieUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.resolver.DefaultAddressResolverGroup;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@RequiredArgsConstructor
@Component
public class AuthenticationComponent {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OauthProperties oauthProperties;
    private static final String WIRETAP_PROPERTY = "reactor.netty.http.client.HttpClient";

    public AuthenticationResponse getOauthTokens(
          String registrationId,
          String authorizationCode,
          String codeVerifier,
          HttpServletResponse response) {

        var registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        var result = askForOauthTokens(registration, authorizationCode, codeVerifier);
        generateSecureCookies(response, result);

        return authenticationResponseBuilder(
              registration.getProviderDetails().getJwkSetUri(),
              result);
    }


    public AuthenticationResponse refreshTokens(String registrationId, HttpServletRequest request,
          HttpServletResponse response) {
        var registration = clientRegistrationRepository.findByRegistrationId(registrationId);

        var refreshTokenKey = oauthProperties.getCookie().getName() + oauthProperties.getToken()
              .getRefreshTokenKey();

        var cookie = CookieUtil.getCookie(request, refreshTokenKey)
              .orElseThrow();

        var cookieValue = CookieUtil.base64Decode(cookie.getValue());

        var result = refreshTokenFlow(registration, cookieValue);

        generateSecureCookies(response, result);

        return authenticationResponseBuilder(
              registration.getProviderDetails().getJwkSetUri(),
              result);
    }

    private void generateSecureCookies(HttpServletResponse response, JsonNode authResponse) {
        if (authResponse != null) {
            generateAccessTokenCookie(response, authResponse);
            generateRefreshTokenCookie(response, authResponse);
        }
    }

    private void generateAccessTokenCookie(HttpServletResponse response, JsonNode authResponse) {
        var accessTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getAccessTokenKey();

        var accessTokenKey = oauthProperties.getToken().getAccessTokenKey();
        var accessToken = authResponse.get(accessTokenKey);
        if (!accessToken.isNull()) {
            CookieUtil.addCookie(
                  response,
                  accessTokenCookieName,
                  CookieUtil.base64Encode(accessToken.asText()),
                  authResponse.get(oauthProperties.getToken().getAccessTokenMaxLifeSecondsKey())
                        .asInt(),
                  oauthProperties.getCookie().getPath(),
                  true);
        }
    }

    private void generateRefreshTokenCookie(HttpServletResponse response, JsonNode authResponse) {
        var refreshTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getRefreshTokenKey();

        var refreshTokenKey = oauthProperties.getToken().getRefreshTokenKey();
        var refreshToken = authResponse.get(refreshTokenKey);
        if (!refreshToken.isNull()) {
            CookieUtil.addCookie(
                  response,
                  refreshTokenCookieName,
                  CookieUtil.base64Encode(refreshToken.asText()),
                  authResponse.get(
                              oauthProperties.getToken().getRefreshTokenMaxLifeSecondsKey())
                        .asInt(),
                  oauthProperties.getCookie().getPath(),
                  true);
        }
    }

    private AuthenticationResponse authenticationResponseBuilder(
          String jwkSetUri,
          JsonNode authResponse) {

        var json = Optional.ofNullable(authResponse)
              .orElseThrow();

        var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        var idToken = json.get(
              oauthProperties.getToken().getIdTokenKey()).asText();

        var decodedIdToken = decoder.decode(idToken);

        var accessTokenMaxLife = json.get(
              oauthProperties.getToken().getAccessTokenMaxLifeSecondsKey()).asInt();

        var refreshTokenMaxLife = json.get(
              oauthProperties.getToken().getRefreshTokenMaxLifeSecondsKey()).asInt();

        return new AuthenticationResponse(
              CookieUtil.base64Encode(decodedIdToken.getClaims()),
              accessTokenMaxLife,
              refreshTokenMaxLife
        );
    }

    private JsonNode askForOauthTokens(
          ClientRegistration registration,
          String authorizationCode,
          String codeVerifier) {

        var parameters = authorizationCodeParameters(
              authorizationCode,
              registration.getRedirectUri(),
              codeVerifier);

        return ssoIssuerRequest(registration, parameters);
    }

    private JsonNode refreshTokenFlow(ClientRegistration registration, String refreshToken) {
        var parameters = refreshTokenParameters(refreshToken);
        return ssoIssuerRequest(registration, parameters);
    }

    private WebClient webClient(ClientRegistration registration) {
        var httpClient = HttpClient
              .create()
              .resolver(DefaultAddressResolverGroup.INSTANCE)
              .wiretap(WIRETAP_PROPERTY,
                    LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL);

        return WebClient.builder()
              .clientConnector(new ReactorClientHttpConnector(httpClient))
              .defaultHeaders(header ->
                    header.setBasicAuth(
                          registration.getClientId(),
                          registration.getClientSecret()
                    )
              )
              .build();
    }

    private JsonNode ssoIssuerRequest(
          ClientRegistration registration,
          MultiValueMap<String, String> parameters) {

        var response = webClient(registration).post()
              .uri(registration.getProviderDetails().getTokenUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters.fromFormData(parameters))
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block();

        if (response != null) {
            return response;
        }
        throw new HttpServerErrorException(HttpStatusCode.valueOf(400));
    }

    private MultiValueMap<String, String> authorizationCodeParameters(
          String code,
          String redirectUri,
          String codeVerifier) {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(AuthConstants.GRANT_TYPE_KEY, AuthConstants.GRANT_TYPE_AUTHORIZATION_CODE);
        formData.add(AuthConstants.CODE_KEY, code);
        formData.add(AuthConstants.REDIRECT_URI_KEY, redirectUri);
        if (codeVerifier != null) {
            formData.add(AuthConstants.CODE_VERIFIER_KEY, codeVerifier);
        }
        return formData;
    }

    private MultiValueMap<String, String> refreshTokenParameters(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(AuthConstants.GRANT_TYPE_KEY, AuthConstants.GRANT_TYPE_REFRESH_TOKEN);
        formData.add(AuthConstants.GRANT_TYPE_REFRESH_TOKEN, refreshToken);
        return formData;
    }
}
