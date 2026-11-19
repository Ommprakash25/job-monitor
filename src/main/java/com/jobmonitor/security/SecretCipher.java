package com.jobmonitor.security;

import com.jobmonitor.config.JobMonitorProperties;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Encrypts mailbox passwords and sign-in tokens before they go into the database
 * (AES-256-GCM). The key lives outside the database: in a key file in your home
 * folder, created on first start, or in the JOBMONITOR_SECURITY_KEY setting.
 */
@Component
public class SecretCipher {

    private static final Logger log = LoggerFactory.getLogger(SecretCipher.class);

    private static final String PREFIX = "v1:";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SecretCipher(JobMonitorProperties properties) {
        this(loadKey(properties.security()));
    }

    public SecretCipher(byte[] rawKey) {
        if (rawKey == null || rawKey.length != KEY_BYTES) {
            throw new IllegalArgumentException("The encryption key must be 32 bytes.");
        }
        this.key = new SecretKeySpec(rawKey, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] stored = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(stored);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Couldn't encrypt the secret.", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalArgumentException("This value wasn't encrypted by job-monitor.");
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Couldn't decrypt a saved password. Was the encryption key file replaced or deleted?", e);
        }
    }

    static byte[] loadKey(JobMonitorProperties.Security security) {
        if (security != null && security.key() != null && !security.key().isBlank()) {
            return decodeKey(security.key().trim(), "the JOBMONITOR_SECURITY_KEY setting");
        }
        String keyFile = security != null && security.keyFile() != null && !security.keyFile().isBlank()
                ? security.keyFile()
                : System.getProperty("user.home") + "/.job-monitor/secret.key";
        return loadOrCreateKeyFile(Path.of(keyFile));
    }

    public static byte[] loadOrCreateKeyFile(Path file) {
        Path path = file.toAbsolutePath();
        try {
            if (Files.exists(path)) {
                return decodeKey(Files.readString(path).trim(), path.toString());
            }
            byte[] raw = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(raw);
            Files.createDirectories(path.getParent());
            Files.writeString(path, Base64.getEncoder().encodeToString(raw), StandardOpenOption.CREATE_NEW);
            log.warn("Created a new encryption key at {}. Back it up: without it, saved mailbox passwords can't be read.",
                    path);
            return raw;
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read or create the encryption key file " + path, e);
        }
    }

    private static byte[] decodeKey(String base64, String source) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("The encryption key in " + source + " isn't valid base64.", e);
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException("The encryption key in " + source + " must be 32 bytes long.");
        }
        return raw;
    }
}
