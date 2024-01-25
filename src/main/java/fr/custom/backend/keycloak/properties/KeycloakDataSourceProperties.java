package fr.custom.backend.keycloak.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@Setter
@Getter
@ConfigurationProperties(prefix = "keycloak.datasource")
public class KeycloakDataSourceProperties {

    private String url;
    private String driverClassName;
    private String username;
    private String password;
}
