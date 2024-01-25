package fr.custom.backend.keycloak.constants;

public class KeycloakConstants {

    private KeycloakConstants() {
        throw new IllegalStateException("Keycloak utility class");
    }

    public static final String ADMIN_USERNAME = "keycloak.embed.admin.user.username";
    public static final String ADMIN_PASSWORD = "keycloak.embed.admin.user.password";
    public static final String REALM_IMPORT_FILE_PATH = "keycloak.embed.realm.file.path";
    public static final String THEME_DIR = "keycloak.theme.dir";
    public static final String REALM_NAME = "keycloak.embed.realm.name";
    public static final String REALM_APP_CLIENT_ID = "keycloak.embed.client.id";
    public static final String REALM_APP_CLIENT_SECRET = "keycloak.embed.client.secret";
    public static final String DEFAULT_CLIENT_ID = "APP_LOCAL_CLIENT";
    public static final String DATABASE_URL = "keycloak.connectionsJpa.url";
    public static final String DATABASE_DRIVER = "keycloak.connectionsJpa.driver";
    public static final String DATABASE_USERNAME = "keycloak.connectionsJpa.user";
    public static final String DATABASE_PASSWORD = "keycloak.connectionsJpa.password";
}
