package com.indiahikes.trekops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Login is handled by AuthService, so drop Spring's generated default user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class TrekOpsApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrekOpsApplication.class, args);
  }
}
