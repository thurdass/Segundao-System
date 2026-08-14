package com.thurdass.system2a.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key; private final long expirationMillis;
    public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-hours:24}") long hours){
        if(secret.length()<32) throw new IllegalArgumentException("JWT secret must have at least 32 characters"); key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); expirationMillis=hours*3_600_000L;
    }
    public String generate(UserDetails user){Instant now=Instant.now(); return Jwts.builder().subject(user.getUsername()).issuedAt(Date.from(now)).expiration(new Date(now.toEpochMilli()+expirationMillis)).signWith(key).compact();}
    public String username(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}
    public boolean valid(String token,UserDetails user){try{return user.getUsername().equalsIgnoreCase(username(token)) && Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getExpiration().after(new Date());}catch(JwtException|IllegalArgumentException e){return false;}}
}
