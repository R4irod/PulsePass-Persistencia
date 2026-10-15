package com.pulsepass.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {

    // Sin el token: NFR-SEC001 prohíbe JWT en logs.
    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}