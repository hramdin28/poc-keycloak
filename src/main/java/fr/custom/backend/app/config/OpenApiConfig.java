package fr.custom.backend.app.config;


import fr.custom.backend.app.properties.AppProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class OpenApiConfig {

    private final AppProperties properties;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
              .components(new Components() {
              })
              .info(new Info()
                    .title(properties.getName())
                    .description(properties.getDescription()));

    }
}
