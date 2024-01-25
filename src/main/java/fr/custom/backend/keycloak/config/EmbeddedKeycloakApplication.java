package fr.custom.backend.keycloak.config;

import fr.custom.backend.keycloak.constants.KeycloakConstants;
import fr.custom.backend.keycloak.properties.AdminUserProperties;
import fr.custom.backend.keycloak.properties.RealmProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;


public class EmbeddedKeycloakApplication extends KeycloakApplication {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);

    @Override
    protected void loadConfig() {
        JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
        Config.init(factory.create()
              .orElseThrow(() -> new NoSuchElementException("No value present")));
    }

    @Override
    protected ExportImportManager bootstrap() {
        final ExportImportManager exportImportManager = super.bootstrap();
        createMasterRealmAdminUser();
        createAppRealm();
        clearKeycloakServerProperties();
        return exportImportManager;
    }

    private void createMasterRealmAdminUser() {

        KeycloakSession session = getSessionFactory().create();

        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

        try {
            var adminUserProperties = adminUserProperties();
            session.getTransactionManager().begin();
            applianceBootstrap.createMasterRealmUser(adminUserProperties.getUsername(),
                  adminUserProperties.getPassword());
            session.getTransactionManager().commit();
        } catch (Exception ex) {
            LOG.warn("Couldn't create keycloak master admin user: {}", ex.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }

    private void createAppRealm() {
        KeycloakSession session = getSessionFactory().create();

        try {
            var realmProperties = realmProperties();

            if (shouldCreateCustomRealm(realmProperties)) {

                session.getTransactionManager()
                      .begin();

                RealmManager manager = new RealmManager(session);
                var lessonRealmImportFile = new ClassPathResource(
                      realmProperties.getRealmImportFile());

                var realmRepresentation = realmRepresentation(
                      lessonRealmImportFile.getInputStream(), realmProperties);

                manager.importRealm(realmRepresentation);

                session.getTransactionManager()
                      .commit();
            }

        } catch (Exception ex) {
            LOG.warn("Failed to import Realm json file: {}", ex.getMessage());
            session.getTransactionManager()
                  .rollback();
        }

        session.close();
    }

    private RealmRepresentation realmRepresentation(
          InputStream inputStream,
          RealmProperties realmProperties) throws IOException {

        var realmRepresentation = JsonSerialization.readValue(inputStream,
              RealmRepresentation.class);

        realmRepresentation.setRealm(realmProperties.getRealmName());
        realmRepresentation.getClients()
              .stream()
              .filter(clientRepresentation -> KeycloakConstants.DEFAULT_CLIENT_ID.equals(
                    clientRepresentation.getClientId()))
              .findFirst()
              .ifPresent(clientRepresentation -> {
                  clientRepresentation.setName(realmProperties.getRealmAppClientId());
                  clientRepresentation.setSecret(realmProperties.getRealmAppClientSecret());
              });

        return realmRepresentation;
    }

    private AdminUserProperties adminUserProperties() {
        var username = System.getProperty(KeycloakConstants.ADMIN_USERNAME);
        var password = System.getProperty(KeycloakConstants.ADMIN_PASSWORD);

        var adminUserProperties = new AdminUserProperties();
        adminUserProperties.setUsername(username);
        adminUserProperties.setPassword(password);
        return adminUserProperties;
    }

    private RealmProperties realmProperties() {
        var realmImportFile = System.getProperty(KeycloakConstants.REALM_IMPORT_FILE_PATH);
        var realmName = System.getProperty(KeycloakConstants.REALM_NAME);
        var realmAppClientId = System.getProperty(KeycloakConstants.REALM_APP_CLIENT_ID);
        var realmAppClientSecret = System.getProperty(
              KeycloakConstants.REALM_APP_CLIENT_SECRET);

        var realmProperties = new RealmProperties();
        realmProperties.setRealmImportFile(realmImportFile);
        realmProperties.setRealmName(realmName);
        realmProperties.setRealmAppClientId(realmAppClientId);
        realmProperties.setRealmAppClientSecret(realmAppClientSecret);

        return realmProperties;
    }

    private boolean shouldCreateCustomRealm(RealmProperties realmProperties) {
        if (realmProperties == null) {
            return false;
        }
        return realmProperties.getRealmImportFile() != null
              && realmProperties.getRealmName() != null
              && realmProperties.getRealmAppClientId() != null
              && realmProperties.getRealmAppClientSecret() != null;
    }

    private void clearKeycloakServerProperties() {
        System.clearProperty(KeycloakConstants.ADMIN_USERNAME);
        System.clearProperty(KeycloakConstants.ADMIN_PASSWORD);

        System.clearProperty(KeycloakConstants.REALM_IMPORT_FILE_PATH);
        System.clearProperty(KeycloakConstants.REALM_NAME);
        System.clearProperty(KeycloakConstants.REALM_APP_CLIENT_ID);
        System.clearProperty(KeycloakConstants.REALM_APP_CLIENT_SECRET);
    }
}
