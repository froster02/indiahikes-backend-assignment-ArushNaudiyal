package com.indiahikes.trekops.exception;

/** The requested entity does not exist. Becomes HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
