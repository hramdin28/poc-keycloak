package fr.custom.backend.app.security.components;

import fr.custom.backend.app.properties.OauthProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Getter
@Component
public class AuthenticationManagerComponent {

    private final Map<String, AuthenticationManager> authenticationManagerMap = new ConcurrentHashMap<>();

    private final OauthProperties oauthProperties;


    public void addManager(String issuer) {
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider
              (JwtDecoders.fromIssuerLocation(issuer));
        authenticationProvider.setJwtAuthenticationConverter(getJwtAuthenticationConverter());
        authenticationManagerMap.put(issuer, authenticationProvider::authenticate);
    }

    private JwtAuthenticationConverter getJwtAuthenticationConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        grantedAuthoritiesConverter.setAuthoritiesClaimName(
              oauthProperties.getToken().getAuthoritiesClaimName());

        grantedAuthoritiesConverter.setAuthorityPrefix(StringUtils.EMPTY);

        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return authenticationConverter;
    }
}
