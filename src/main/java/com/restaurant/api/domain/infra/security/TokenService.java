package com.restaurant.api.domain.infra.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.exception.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {
  @Value("${api.security.secret.key}")
  private String secret;
  @Value("${api.security.secret.issuer}")
  private String issuer;


  public String generateToken(User user){
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      return JWT.create()
          .withIssuer(issuer)
          .withSubject(user.getLogin())
          .withExpiresAt(generateExpirationDate())
          .sign(algorithm);
    }catch(JWTCreationException e){
      throw new ConflictException(
          "Error in creating JWT Token",
          "Error while generating token: " + e.getMessage());
    }
  }

  public String validateToken(String token){
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      return JWT.require(algorithm)
          .withIssuer(issuer)
          .build()
          .verify(token)
          .getSubject();
    }catch(JWTVerificationException e){
          return "";
    }
  }

  private Instant generateExpirationDate(){
    return Instant.now().plusSeconds(3600);
  }
}
