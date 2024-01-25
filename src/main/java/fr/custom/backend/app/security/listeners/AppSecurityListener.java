package fr.custom.backend.app.security.listeners;

import fr.custom.backend.app.properties.OauthProperties;
import fr.custom.backend.app.security.components.AuthenticationManagerComponent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

@Profile("!test")
@RequiredArgsConstructor
@Configuration
public class AppSecurityListener {

    private final AuthenticationManagerComponent authenticationManagerComponent;
    private final OauthProperties oauthProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void lazyLoadOauthIssuers() {
        var issuers = List.of(oauthProperties.getConnection().getIssuerUri());
        issuers.forEach(authenticationManagerComponent::addManager);
    }
}
