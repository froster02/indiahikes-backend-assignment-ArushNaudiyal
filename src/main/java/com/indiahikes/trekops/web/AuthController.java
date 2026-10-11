package com.indiahikes.trekops.web;

import com.indiahikes.trekops.service.AuthService;
import com.indiahikes.trekops.web.dto.LoginRequest;
import com.indiahikes.trekops.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth")
@SecurityRequirements
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    AuthService.LoginResult result = authService.login(request.username(), request.password());
    return new LoginResponse(result.token(), "Bearer", result.expiresIn().toSeconds());
  }
}
