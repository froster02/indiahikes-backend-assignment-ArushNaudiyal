package com.indiahikes.trekops.exception;

/** The request clashes with existing data, such as a duplicate check-in. Becomes HTTP 409. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
