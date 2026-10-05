package br.edu.pucgoias.linkhealth.infrastructure.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Criptografa fotos com AES-256-GCM antes de gravá-las no disco ou no banco.
 *
 * <p>A chave deve existir somente na variável de ambiente
 * LINK_HEALTH_PHOTOS_ENCRYPTION_KEY e precisa conter 32 bytes codificados em Base64.</p>
 */
@Component
public class PhotoEncryptionService {

    private static final byte CURRENT_VERSION = 1;
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int AUTHENTICATION_TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhotoEncryptionService(
            @Value("${link-health.photos.encryption-key}") String encodedKey) {
        this.secretKey = new SecretKeySpec(decodeAndValidateKey(encodedKey), "AES");
    }

    public byte[] encrypt(InputStream content) throws IOException {
        try {
            byte[] plainText = content.readAllBytes();
            byte[] nonce = new byte[NONCE_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(AUTHENTICATION_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText);
            ByteBuffer payload = ByteBuffer.allocate(1 + NONCE_BYTES + encrypted.length);
            payload.put(CURRENT_VERSION);
            payload.put(nonce);
            payload.put(encrypted);
            return payload.array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível criptografar a foto de evolução.", exception);
        }
    }

    public byte[] decrypt(byte[] payload) {
        if (payload == null || payload.length <= 1 + NONCE_BYTES || payload[0] != CURRENT_VERSION) {
            throw new IllegalStateException("O arquivo criptografado da foto é inválido.");
        }

        try {
            byte[] nonce = Arrays.copyOfRange(payload, 1, 1 + NONCE_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, 1 + NONCE_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(AUTHENTICATION_TAG_BITS, nonce));
            return cipher.doFinal(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível verificar ou descriptografar a foto de evolução.", exception);
        }
    }

    public int currentVersion() {
        return CURRENT_VERSION;
    }

    private byte[] decodeAndValidateKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "Defina LINK_HEALTH_PHOTOS_ENCRYPTION_KEY antes de iniciar a API.");
        }
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != KEY_BYTES) {
                throw new IllegalStateException(
                        "LINK_HEALTH_PHOTOS_ENCRYPTION_KEY deve representar exatamente 32 bytes em Base64.");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "LINK_HEALTH_PHOTOS_ENCRYPTION_KEY deve estar no formato Base64.", exception);
        }
    }
}
