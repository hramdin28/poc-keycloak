package fr.custom.backend.keycloak.config;

import java.util.Properties;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.services.util.JsonConfigProviderFactory;

public class RegularJsonConfigProviderFactory extends JsonConfigProviderFactory {

    @Override
    protected Properties getProperties() {
        return new SystemEnvProperties(System.getenv());
    }
}
