package fr.custom.backend.app.expositions.controllers;


import fr.custom.backend.app.expositions.dtos.UserDto;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    @GetMapping("/all")
    public List<UserDto> users(@AuthenticationPrincipal OAuth2User principal) {

        var user1 = UserDto.builder()
              .firstname("John")
              .lastname("Doe")
              .age(23)
              .build();

        var user2 = UserDto.builder()
              .firstname("Jane")
              .lastname("Doe")
              .age(19)
              .build();

        return List.of(user1, user2);
    }
}
