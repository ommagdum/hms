package com.vinayakit.hms.security.jwt;

import com.vinayakit.hms.security.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsImpl userDetails;

    private final String jwtSecret = "mySecretKeyForJWTGenerationWithAtLeast32CharactersLong!";
    private final String jwtExpirationMs = "86400000"; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", jwtExpirationMs);
    }

    @Test
    void generateJwtToken_Success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtils.generateJwtToken(authentication);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Validate the token
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();

        // Extract username
        String extractedUsername = jwtUtils.getUserNameFromJwtToken(token);
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void validateJwtToken_ValidToken_ReturnsTrue() {
        // Create a token directly using the same secret
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
    }

    @Test
    void validateJwtToken_ExpiredToken_ReturnsFalse() {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
    }

    @Test
    void validateJwtToken_MalformedToken_ReturnsFalse() {
        // Too many parts (4) -> MalformedJwtException
        String tooManyParts = "header.payload.signature.extra";
        assertThat(jwtUtils.validateJwtToken(tooManyParts)).isFalse();

        // Random string without dots -> MalformedJwtException
        String randomString = "not.a.jwt.token"; // actually has 3 dots, still malformed
        assertThat(jwtUtils.validateJwtToken(randomString)).isFalse();

        // Well-formed JWT with wrong signature -> SignatureException (now caught)
        String wrongSignature = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        assertThat(jwtUtils.validateJwtToken(wrongSignature)).isFalse();
    }

    @Test
    void validateJwtToken_UnsupportedToken_ReturnsFalse() {
        String unsupported = ""; // empty
        assertThat(jwtUtils.validateJwtToken(unsupported)).isFalse();

    }

    @Test
    void validateJwtToken_EmptyClaims_ReturnsFalse() {
        // IllegalArgumentException case
        assertThat(jwtUtils.validateJwtToken(null)).isFalse(); // null should cause IllegalArgumentException
    }

    @Test
    void getUserNameFromJwtToken_ValidToken_ReturnsUsername() {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String username = jwtUtils.getUserNameFromJwtToken(token);
        assertThat(username).isEqualTo("testuser");
    }
}