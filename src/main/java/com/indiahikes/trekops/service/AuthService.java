package com.indiahikes.trekops.service;

import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.repository.UserRepository;
import java.time.Duration;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Checks a username and password and issues a token. */
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  // Compared against when the username is unknown, so both failures take about the same time.
  private final String dummyHash;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  /**
   * @throws BadCredentialsException for an unknown user or a wrong password, with the same message
   */
  public LoginResult login(String username, String password) {
    User user = userRepository.findByUsername(username).orElse(null);
    String hash = user != null ? user.getPasswordHash() : dummyHash;
    boolean passwordMatches = passwordEncoder.matches(password, hash);
    if (user == null || !passwordMatches) {
      throw new BadCredentialsException("Invalid username or password");
    }
    String token = jwtService.generateToken(user.getUsername(), user.getRole());
    return new LoginResult(token, jwtService.getTtl());
  }

  public record LoginResult(String token, Duration expiresIn) {}
}
