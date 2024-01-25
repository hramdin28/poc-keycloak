package fr.custom.backend.keycloak.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@Setter
@Getter
@ConfigurationProperties(prefix = "keycloak.server")
public class KeycloakServerProperties {

    private String contextPath;

    private AdminUserProperties adminUser;

    private RealmProperties realm;

    private String themeDir;
}
