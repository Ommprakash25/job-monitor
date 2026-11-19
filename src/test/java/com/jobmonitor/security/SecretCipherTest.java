package com.jobmonitor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecretCipherTest {

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndDecryptsBack() {
        SecretCipher cipher = new SecretCipher(SecretCipher.loadOrCreateKeyFile(tempDir.resolve("k.key")));
        String stored = cipher.encrypt("abcdefghijklmnop");
        assertThat(stored).startsWith("v1:").doesNotContain("abcdefghijklmnop");
        assertThat(cipher.decrypt(stored)).isEqualTo("abcdefghijklmnop");
    }

    @Test
    void sameSecretEncryptsDifferentlyEachTime() {
        SecretCipher cipher = new SecretCipher(SecretCipher.loadOrCreateKeyFile(tempDir.resolve("k.key")));
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void rejectsTamperedValues() {
        SecretCipher cipher = new SecretCipher(SecretCipher.loadOrCreateKeyFile(tempDir.resolve("k.key")));
        String stored = cipher.encrypt("secret");
        String tampered = stored.substring(0, stored.length() - 4) + (stored.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createsTheKeyFileOnceAndReusesIt() throws Exception {
        Path keyFile = tempDir.resolve("nested/secret.key");
        byte[] first = SecretCipher.loadOrCreateKeyFile(keyFile);
        assertThat(Files.exists(keyFile)).isTrue();
        assertThat(SecretCipher.loadOrCreateKeyFile(keyFile)).isEqualTo(first);
    }

    @Test
    void anotherKeyCantReadIt() {
        String stored = new SecretCipher(SecretCipher.loadOrCreateKeyFile(tempDir.resolve("a.key"))).encrypt("secret");
        SecretCipher other = new SecretCipher(SecretCipher.loadOrCreateKeyFile(tempDir.resolve("b.key")));
        assertThatThrownBy(() -> other.decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }
}
