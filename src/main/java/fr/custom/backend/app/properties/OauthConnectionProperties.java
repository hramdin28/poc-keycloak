package fr.custom.backend.app.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OauthConnectionProperties {

    private String issuerUri;
    private String authUri;
    private String jwksUri;
    private String tokenUri;
    private String logoutUri;
}
