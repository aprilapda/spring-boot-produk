package com.apda.order.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${app.jwt.signing-key}")
    private String signingKey;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    // Membuat token: isi (claims) = username + role + waktu kedaluwarsa
    public String generateToken(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)                 // siapa pemegang token
                .claim("role", role)               // hak akses
                .issuedAt(now)                     // dibuat kapan
                .expiration(new Date(now.getTime() + expirationMs))  // expired kapan
                .signWith(key())                   // ditandatangani (HS256)
                .compact();
    }
    // Ambil isi token (username, role, exp) - melempar exception kalau token palsu/rusak
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)          // verifikasi tanda tangan + expired
                .getPayload();
    }

    // Cek token masih berlaku atau tidak (tanda tangan benar & belum expired)
    public boolean isValid(String token) {
        try {
            return !parseClaims(token).getExpiration().before(new Date());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(signingKey.getBytes());
    }
}
