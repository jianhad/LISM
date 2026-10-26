package cn.lims.auth;

import cn.lims.auth.AuthController.LoginResponse;
import cn.lims.config.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, JwtProperties properties) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public LoginResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenSeconds(), ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("permissions", authentication.getAuthorities().stream().map(Object::toString).toList())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt);
    }
}

