// import java.math.BigInteger;
import java.security.KeyPairGenerator;
import javax.crypto.KeyGenerator;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.Base64;
import java.util.HashMap;


public class Encryption {
  
  private final KeyPair keyPair;
  // public static final BigInteger e = BigInteger.valueOf(65537);
  // private final int keysize;
  // private final Cipher cipherEncrypt;
  private final Cipher cipherDecrypt;
  private Map<String, SecretKey> AESKeys = new HashMap<String, SecretKey>(); 
  private final int keysize;

  public PublicKey getPublicKey() {
    return keyPair.getPublic();
  }

  
  /* private void keyGenerator() {
    BigInteger p;
    BigInteger q;
    BigInteger qMinusOne;
    BigInteger pMinusOne;
    BigInteger lambda;

    while (true) {
      p = BigInteger.probablePrime(keysize, rnd);
      q = BigInteger.probablePrime(keysize, rnd);
      pMinusOne = p.subtract(BigInteger.ONE);
      qMinusOne = q.subtract(BigInteger.ONE);
      lambda = pMinusOne.multiply(qMinusOne).divide(pMinusOne.gcd(qMinusOne));
      if (p.isProbablePrime(certainty)
          && q.isProbablePrime(certainty)
          && e.gcd(lambda).equals(BigInteger.ONE))
      {
        break;
      }
    }

    PublicKey = p.multiply(q);
    PrivateKey = e.modInverse(lambda);
  } */


  public void decryptAESKey(String username, byte[] AESKeyCiphertext) throws Exception {
    AESKeys.put(username, new SecretKeySpec(cipherDecrypt.doFinal(AESKeyCiphertext), "AES"));
  }
  
  public byte[] createAndEncryptAESKey(String username, PublicKey key) throws Exception {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(keysize);
    SecretKey aesKey = keyGenerator.generateKey();
    AESKeys.put(username, aesKey);
    Cipher cipherEncrypt = Cipher.getInstance("ECIES/PCBC/OAEPPadding");
    cipherEncrypt.init(Cipher.ENCRYPT_MODE, key);
    return cipherEncrypt.doFinal(aesKey.getEncoded());
  }

  public String encryptMessage(String message, String username) throws Exception {
    byte[] plaintextBytes = message.getBytes(StandardCharsets.UTF_8);
    SecretKey aesKey = AESKeys.get(username);
    Cipher cipherEncrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipherEncrypt.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] cipherBytes = cipherEncrypt.doFinal(plaintextBytes);
    byte[] b64cipherBytes = Base64.getEncoder().encode(cipherBytes);
    // encoding doesn't matter because for b64 it's all the same
    return new String(b64cipherBytes, StandardCharsets.UTF_8);
  }

  public String decryptMessage(String messageCiphertext, String username) throws Exception {
    byte[] b64cipherBytes = messageCiphertext.getBytes(StandardCharsets.UTF_8);
    byte[] cipherBytes = Base64.getDecoder().decode(b64cipherBytes);
    SecretKey aesKey = AESKeys.get(username);
    Cipher cipherDecrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipherDecrypt.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] plaintextBytes = cipherDecrypt.doFinal(cipherBytes);
    return new String(plaintextBytes, StandardCharsets.UTF_8);
  }

  // keysize is in number of bits 
  public Encryption(int keysize) throws Exception {
    this.keysize = keysize;
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    keyPairGenerator.initialize(keysize);
    keyPair = keyPairGenerator.genKeyPair();
    cipherDecrypt = Cipher.getInstance("ECIES/PCBC/OAEPPadding");
    cipherDecrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
  }
}
