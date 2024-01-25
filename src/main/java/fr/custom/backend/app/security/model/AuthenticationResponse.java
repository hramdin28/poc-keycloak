package fr.custom.backend.app.security.model;

public record AuthenticationResponse(String idToken, int expires_in, int refresh_expires_in) {

}

