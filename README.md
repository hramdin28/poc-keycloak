# Keycloak embedded module

## Documentation

- [Keycloak Embedded in a Spring Boot Application](https://www.baeldung.com/keycloak-embedded-in-spring-boot-app)
- [Customizing Themes for Keycloak](https://www.baeldung.com/spring-keycloak-custom-themes#embedded)

## Configuration

In your application.yml, you must have the following variables set:

```
keycloak:
  datasource:
    url: xxxx
    driverClassName: xxx
    username: user
    password: pwd
  server:
    contextPath: /auth
    adminUser:
      username: admin
      password: pwd
    realm:
      realmImportFile: realm-export.json
      realmName: APP_LOCAL
      realmAppClientId: APP_LOCAL_CLIENT
      realmAppClientSecret: appLocalSecret
```

- Keycloak configuration params are located in the `ressources/META-INF/keycloak-server.json`
- The params can be overrided using environment variables
