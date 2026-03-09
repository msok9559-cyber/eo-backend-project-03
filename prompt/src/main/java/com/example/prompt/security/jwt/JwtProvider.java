package com.example.prompt.security.jwt;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.secretKey = new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );

        log.info("JWT secret loaded");
    }

    /**
     * 관리자 Access Token 발급
     */
    public String createAdminToken(String adminId) {
        return Jwts.builder()
                .subject(adminId)
                .claim("type", "ADMIN")
                .issuedAt(new Date())
                .expiration(getDateAfterDuration(jwtProperties.expiration()))
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT에서 adminId(subject) 추출
     */
    public String getAdminId(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * JWT에서 type 추출
     */
    public String getType(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("type", String.class);
    }

    /**
     * JWT 만료 여부 확인
     */
    public boolean isExpired(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .before(new Date());
    }

    /**
     * JWT 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            return !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Date getDateAfterDuration(java.time.Duration duration) {
        return new Date(new Date().getTime() + duration.toMillis());
    }
}