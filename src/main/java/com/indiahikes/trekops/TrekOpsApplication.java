package com.indiahikes.trekops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TrekOpsApplication {

  public static void main(String[] args) {
    SpringApplication.run(TrekOpsApplication.class, args);
  }
}
