package fr.custom.backend.app.security;

import static fr.aefe.scola.app.security.constants.AuthConstants.CODE_KEY;
import static fr.aefe.scola.app.security.constants.AuthConstants.CODE_VERIFIER_KEY;
import static fr.aefe.scola.app.security.constants.AuthConstants.GRANT_TYPE_AUTHORIZATION_CODE;
import static fr.aefe.scola.app.security.constants.AuthConstants.GRANT_TYPE_KEY;
import static fr.aefe.scola.app.security.constants.AuthConstants.GRANT_TYPE_REFRESH_TOKEN;
import static fr.aefe.scola.app.security.constants.AuthConstants.REDIRECT_URI_KEY;
import com.fasterxml.jackson.databind.JsonNode;
import fr.aefe.scola.app.properties.OauthProperties;
import fr.aefe.scola.app.security.model.AuthenticationResponse;
import fr.aefe.scola.app.utils.CookieUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.resolver.DefaultAddressResolverGroup;
import jakarta.servlet.http.Cookie;
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
    private static final String NO_CONTENT = "NO CONTENT";

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

        var refreshTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getRefreshTokenKey();

        var cookie = CookieUtil.getCookie(request, refreshTokenCookieName)
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

    public void logout(HttpServletRequest request, HttpServletResponse response,
          String registrationId) {

        var accessTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getAccessTokenKey();

        var refreshTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getRefreshTokenKey();

        var cookieValue = CookieUtil.getCookie(request, refreshTokenCookieName)
              .map(Cookie::getValue)
              .orElseThrow();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(oauthProperties.getToken().getRefreshTokenKey(),
              CookieUtil.base64Decode(cookieValue));

        logOutRequest(formData, registrationId);

        clearCookies(request, response, accessTokenCookieName, refreshTokenCookieName,
              oauthProperties.getCookie().getPath());
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

    private void logOutRequest(MultiValueMap<String, String> parameters, String registrationId) {
        var registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        webClient(registration).post()
              .uri(oauthProperties.getConnection().getLogoutUri())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(BodyInserters.fromFormData(parameters))
              .retrieve()
              .bodyToMono(String.class)
              .defaultIfEmpty(NO_CONTENT)
              .block();
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
        formData.add(GRANT_TYPE_KEY, GRANT_TYPE_AUTHORIZATION_CODE);
        formData.add(CODE_KEY, code);
        formData.add(REDIRECT_URI_KEY, redirectUri);
        if (codeVerifier != null) {
            formData.add(CODE_VERIFIER_KEY, codeVerifier);
        }
        return formData;
    }

    private MultiValueMap<String, String> refreshTokenParameters(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE_KEY, GRANT_TYPE_REFRESH_TOKEN);
        formData.add(GRANT_TYPE_REFRESH_TOKEN, refreshToken);
        return formData;
    }

    private void clearCookies(HttpServletRequest request, HttpServletResponse response,
          String accessTokenCookieName, String refreshTokenCookieName, String path) {
        CookieUtil.deleteCookie(request, response, accessTokenCookieName,
              path);
        CookieUtil.deleteCookie(request, response, refreshTokenCookieName,
              path);
    }
}
