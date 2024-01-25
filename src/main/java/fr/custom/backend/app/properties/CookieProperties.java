package fr.custom.backend.app.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CookieProperties {

    private String name;
    private String domain;
    private Boolean secure;
    private String path;
    private String sameSite;
}
