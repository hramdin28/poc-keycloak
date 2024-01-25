package fr.custom.backend.app.security.config;

import fr.custom.backend.app.properties.OauthProperties;
import fr.custom.backend.app.security.components.AuthenticationManagerComponent;
import fr.custom.backend.app.utils.CookieUtil;
import fr.custom.backend.keycloak.properties.KeycloakServerProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class CustomSecurity {

    private static final String BEARER_HEADER = "Bearer ";
    private static final String KEYCLOAK_SUFFIX = "/**";
    private static final String URI_TO_APPLY_TOKEN_COOKIE = "/api/";
    private static final String TOKEN_REFRESH_URI = "/auth/v1/refresh-token";


    private final OauthProperties oauthProperties;
    private final KeycloakServerProperties keycloakServerProperties;
    private final AuthenticationManagerComponent authenticationManagerComponent;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
              .requestMatchers(whiteList());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieCsrfTokenRepository.setCookiePath(oauthProperties.getCookie().getPath());

        Map<String, AuthenticationManager> authManager = authenticationManagerComponent.getAuthenticationManagerMap();

        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
              new JwtIssuerAuthenticationManagerResolver(authManager::get);

        return http
              .headers(
                    httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(
                          FrameOptionsConfig::sameOrigin))
              .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
              .csrf(c -> c.ignoringRequestMatchers(keycloakUriPath())
                    .csrfTokenRepository(cookieCsrfTokenRepository))
              .authorizeHttpRequests(a ->
                    a.requestMatchers(
                                "/auth/v1/isLoggedIn",
                                "/auth/v1/oauth-token",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/management/health",
                                "/management/info",
                                "/management/**"
                          )
                          .permitAll()
                          .anyRequest()
                          .authenticated())
              .oauth2ResourceServer(
                    oauth2 ->
                          oauth2.authenticationManagerResolver(authenticationManagerResolver)
                                .bearerTokenResolver(this::tokenExtractor)
              )
              .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
          ClientRegistrationRepository clientRegistrationRepository,
          OAuth2AuthorizedClientService authorizedClientService) {

        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
              .clientCredentials()
              .build();

        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
              clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }


    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults(oauthProperties.getToken().getAuthorityRolePrefix());
    }

    private String keycloakUriPath() {
        return keycloakServerProperties.getContextPath() + KEYCLOAK_SUFFIX;
    }

    public String tokenExtractor(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            return header.replace(BEARER_HEADER, StringUtils.EMPTY);
        }
        return getAccessTokenFromCookie(request);
    }

    private String getAccessTokenFromCookie(HttpServletRequest request) {
        var accessTokenCookieName =
              oauthProperties.getCookie().getName() + oauthProperties.getToken()
                    .getAccessTokenKey();

        return CookieUtil.getCookie(request, accessTokenCookieName)
              .map(cookie -> {
                  if (shouldApplyCookieToUri(request.getRequestURI())) {

                      return CookieUtil.base64Decode(cookie.getValue());
                  }
                  return null;
              })
              .orElse(null);
    }

    private boolean shouldApplyCookieToUri(String requestUri) {
        return Stream.of(URI_TO_APPLY_TOKEN_COOKIE,
                    TOKEN_REFRESH_URI)
              .anyMatch(requestUri::contains);
    }

    private String[] whiteList() {
        return new String[]{
              keycloakUriPath(),
              "keycloak/**"
        };
    }
}
