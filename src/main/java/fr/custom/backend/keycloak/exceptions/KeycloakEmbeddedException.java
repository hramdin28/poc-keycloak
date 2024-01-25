package fr.custom.backend.keycloak.exceptions;

public class KeycloakEmbeddedException extends RuntimeException {

    public KeycloakEmbeddedException(Exception e) {
        super(e);
    }
}
