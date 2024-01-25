package fr.custom.backend.keycloak.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/keycloak/v1/ref", produces = MediaType.APPLICATION_JSON_VALUE)
public class KeycloakOptionsController {

    @GetMapping("/country")
    public List<Map<String, String>> countries() {

        return List.of(
              Map.of("value", "FRA", "text", "France"),
              Map.of("value", "SWI", "text", "Swiss")
        );
    }
}
