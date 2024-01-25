package fr.custom.backend.app.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OauthTokenProperties {

    private String idTokenKey;
    private String accessTokenKey;
    private String accessTokenMaxLifeSecondsKey;
    private String refreshTokenKey;
    private String refreshTokenMaxLifeSecondsKey;
    private String authoritiesClaimName;
    private String authorityRolePrefix;
}
