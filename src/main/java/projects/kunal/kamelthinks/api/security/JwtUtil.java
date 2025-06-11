package projects.kunal.kamelthinks.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component // Make sure this is annotated as a Spring Component
public class JwtUtil {

    @Value("${jwt.secret}") // Assuming you store your secret key in application.properties/yaml
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration; // In milliseconds

    private SecretKey key; // Declare a SecretKey field

    // This method should be called once, e.g., in @PostConstruct or in the constructor
    // to initialize the SecretKey from your string secret.
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        // Initialize the key if it hasn't been already (e.g., if you don't use @PostConstruct)
        if (this.key == null) {
            init();
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                // Use the Key object directly with the modern API
                .signWith(key) // This is the corrected line for jjwt 0.10+
                .compact();
    }

    // You might also need to update parseToken, validateToken methods similarly
    public Claims getClaimsFromToken(String token) {
        if (this.key == null) {
            init();
        }
        return Jwts.parser()
                .setSigningKey(key) // Use the Key object here too
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public boolean validateToken(String token, String username) {
        String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return getClaimsFromToken(token).getExpiration().before(new Date());
    }
}