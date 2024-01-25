package fr.custom.backend.keycloak.config;

import fr.custom.backend.keycloak.constants.KeycloakConstants;
import fr.custom.backend.keycloak.properties.KeycloakDataSourceProperties;
import fr.custom.backend.keycloak.properties.KeycloakServerProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.naming.CompositeName;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.platform.Platform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
public class EmbeddedKeycloakConfig {

    @Bean
    ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(
          KeycloakServerProperties keycloakServerProperties,
          KeycloakDataSourceProperties keycloakDataSourceProperties,
          DataSource dataSource) throws NamingException {

        setKeycloakDataSourceProperties(keycloakDataSourceProperties);
        setKeyCloakServerProperties(keycloakServerProperties);

        mockJndiEnvironment(dataSource);

        ServletRegistrationBean<HttpServlet30Dispatcher> servlet = new ServletRegistrationBean<>(
              new HttpServlet30Dispatcher());
        servlet.addInitParameter("jakarta.ws.rs.Application",
              EmbeddedKeycloakApplication.class.getName());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX,
              keycloakServerProperties.getContextPath());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS,
              "true");
        servlet.addUrlMappings(keycloakServerProperties.getContextPath() + "/*");
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);

        return servlet;
    }

    @Bean
    FilterRegistrationBean<EmbeddedKeycloakRequestFilter> keycloakSessionManagement(
          KeycloakServerProperties keycloakServerProperties) {

        FilterRegistrationBean<EmbeddedKeycloakRequestFilter> filter = new FilterRegistrationBean<>();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new EmbeddedKeycloakRequestFilter());
        filter.addUrlPatterns(keycloakServerProperties.getContextPath() + "/*");

        return filter;
    }

    @Bean("fixedThreadPool")
    public ExecutorService fixedThreadPool() {
        return Executors.newFixedThreadPool(5);
    }

    @Bean
    @ConditionalOnMissingBean(name = "springBootPlatform")
    protected SimplePlatformProvider springBootPlatform() {
        return (SimplePlatformProvider) Platform.getPlatform();
    }

    private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
        NamingManager.setInitialContextFactoryBuilder(env -> environment -> new InitialContext() {

            @Override
            public Object lookup(Name name) {
                return lookup(name.toString());
            }

            @Override
            public Object lookup(String name) {

                if ("spring/datasource".equals(name)) {
                    return dataSource;
                } else if (name.startsWith("java:jboss/ee/concurrency/executor/")) {
                    return fixedThreadPool();
                }

                return null;
            }

            @Override
            public NameParser getNameParser(String name) {
                return CompositeName::new;
            }

            @Override
            public void close() {
                // NOOP
            }
        });
    }

    private void setKeycloakDataSourceProperties(
          KeycloakDataSourceProperties keycloakDataSourceProperties) {

        System.setProperty(KeycloakConstants.DATABASE_URL,
              keycloakDataSourceProperties.getUrl());
        System.setProperty(KeycloakConstants.DATABASE_DRIVER,
              keycloakDataSourceProperties.getDriverClassName());
        System.setProperty(KeycloakConstants.DATABASE_USERNAME,
              keycloakDataSourceProperties.getUsername());
        System.setProperty(KeycloakConstants.DATABASE_PASSWORD,
              keycloakDataSourceProperties.getPassword());
    }

    private void setKeyCloakServerProperties(KeycloakServerProperties keycloakServerProperties) {
        System.setProperty(KeycloakConstants.ADMIN_USERNAME,
              keycloakServerProperties.getAdminUser().getUsername());
        System.setProperty(KeycloakConstants.ADMIN_PASSWORD,
              keycloakServerProperties.getAdminUser().getPassword());
        System.setProperty(KeycloakConstants.THEME_DIR,
              keycloakServerProperties.getThemeDir());
        System.setProperty(KeycloakConstants.REALM_IMPORT_FILE_PATH,
              keycloakServerProperties.getRealm().getRealmImportFile());
        System.setProperty(KeycloakConstants.REALM_NAME,
              keycloakServerProperties.getRealm().getRealmName());
        System.setProperty(KeycloakConstants.REALM_APP_CLIENT_ID,
              keycloakServerProperties.getRealm().getRealmAppClientId());
        System.setProperty(KeycloakConstants.REALM_APP_CLIENT_SECRET,
              keycloakServerProperties.getRealm().getRealmAppClientSecret());
    }
}
