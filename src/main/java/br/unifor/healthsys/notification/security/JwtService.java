package br.unifor.healthsys.notification.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Service
public class JwtService {

    @Value("${jwt.public-key-location}")
    private Resource publicKeyResource;

    private RSAPublicKey publicKey;

    @PostConstruct
    void init() {
        try {
            publicKey = (RSAPublicKey) readPublicKey(publicKeyResource);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao carregar a chave publica do JWT.", e);
        }
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object rawUserId = claims.get("userId");
        Long userId = rawUserId instanceof Number number ? number.longValue() : null;

        return new AuthenticatedUser(
                userId,
                claims.getSubject(),
                claims.get("role", String.class),
                claims.get("email", String.class),
                claims.get("nome", String.class)
        );
    }

    private PublicKey readPublicKey(Resource resource) throws IOException, GeneralSecurityException {
        String pem = resource.getContentAsString(StandardCharsets.UTF_8);
        String content = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = Decoders.BASE64.decode(content);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }
}
