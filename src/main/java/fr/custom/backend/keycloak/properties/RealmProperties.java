package fr.custom.backend.keycloak.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RealmProperties {

    private String realmImportFile;
    private String realmName;
    private String realmAppClientId;
    private String realmAppClientSecret;
}
