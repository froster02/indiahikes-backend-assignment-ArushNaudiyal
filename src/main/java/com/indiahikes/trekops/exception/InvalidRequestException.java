package com.indiahikes.trekops.exception;

/** The request asks for something the API does not allow, such as an unknown sort field. 400. */
public class InvalidRequestException extends RuntimeException {

  public InvalidRequestException(String message) {
    super(message);
  }
}
