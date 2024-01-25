package fr.custom.backend.app.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties("oauth")
public class OauthProperties {

    private OauthConnectionProperties connection;
    private OauthTokenProperties token;
    private CookieProperties cookie;
}
