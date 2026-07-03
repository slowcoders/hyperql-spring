package org.slowcoders.basecamp.app.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.app.model.LoginDTO;
import org.slowcoders.basecamp.security.CustomJwtFilter;
import org.slowcoders.basecamp.security.service.TokenDTO;
import org.slowcoders.basecamp.security.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final NativeWebRequest request;
    private final AuthService authService;

    @GetMapping("/login")
    public TokenDTO login(LoginDTO login) {
        return authService.login(login.getId(), login.getPassword());
    }

    @GetMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = CustomJwtFilter.resolveToken(authHeader);
        authService.logout(token);
    }

}
