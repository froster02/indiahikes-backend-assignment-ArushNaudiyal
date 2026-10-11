package com.indiahikes.trekops.exception;

/** The request is well formed but breaks a business rule. Becomes HTTP 422. */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}
