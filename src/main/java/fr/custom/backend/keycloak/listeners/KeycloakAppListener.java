package fr.custom.backend.keycloak.listeners;


import fr.custom.backend.keycloak.properties.KeycloakServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class KeycloakAppListener {

    private static final Logger LOG = LoggerFactory.getLogger(KeycloakAppListener.class);

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(
          ServerProperties serverProperties,
          KeycloakServerProperties keycloakServerProperties) {

        return evt -> {

            Integer port = serverProperties.getPort();
            String keycloakContextPath = keycloakServerProperties.getContextPath();

            LOG.info("Embedded Keycloak started: http://localhost:{}{} to use keycloak", port,
                  keycloakContextPath);
        };
    }
}
