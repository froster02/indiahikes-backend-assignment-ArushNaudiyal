package com.indiahikes.trekops.web.dto;

/** Send the token back as "Authorization: Bearer token". */
public record LoginResponse(String token, String tokenType, long expiresInSeconds) {}
