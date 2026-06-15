package api.poja.io.endpoint.rest.security.github;

import static com.goterl.lazysodium.interfaces.Box.SEALBYTES;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import java.util.Base64;
import org.bouncycastle.openssl.EncryptionException;
import org.springframework.stereotype.Component;

@Component
public class GithubSecretEncryptor {

  private final LazySodiumJava lazySodium;

  public GithubSecretEncryptor() {
    this.lazySodium = new LazySodiumJava(new SodiumJava());
  }

  private byte[] cryptoBoxSeal(String secretValue, String publicKeyBase64)
      throws EncryptionException {
    byte[] publicKey;
    try {
      publicKey = Base64.getDecoder().decode(publicKeyBase64);
    } catch (IllegalArgumentException e) {
      throw new EncryptionException("Invalid base64 public key format", e);
    }
    byte[] secretBytes = secretValue.getBytes(UTF_8);
    byte[] encryptedBytes = new byte[secretBytes.length + SEALBYTES];
    boolean success =
        lazySodium.cryptoBoxSeal(encryptedBytes, secretBytes, secretBytes.length, publicKey);
    if (!success) {
      throw new EncryptionException("Crypto box seal operation failed");
    }
    return encryptedBytes;
  }

  public String encryptSecretValue(String secretValue, String publicKeyBase64)
      throws EncryptionException {
    if (secretValue == null) {
      throw new EncryptionException("Secret value cannot be null");
    }
    if (publicKeyBase64 == null || publicKeyBase64.trim().isEmpty()) {
      throw new EncryptionException("Public key cannot be null or empty");
    }
    byte[] encryptedBytes = cryptoBoxSeal(secretValue, publicKeyBase64);
    return Base64.getEncoder().encodeToString(encryptedBytes);
  }
}
