// import java.math.BigInteger;
import java.security.KeyPairGenerator;
import javax.crypto.KeyGenerator;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.security.spec.X509EncodedKeySpec;


public class Encryption {
  
  private final KeyPair keyPair;
  private final Cipher cipherDecrypt;
  // maps groupId to AESkey for that group  
  private Map<Integer, SecretKey> AESKeys = new HashMap<Integer, SecretKey>(); 
  private final int keysize;
  private static final String publicKeyAlgorithm = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

  public String getPublicKey() {
    PublicKey key = keyPair.getPublic();
    byte[] keyByteArray = key.getEncoded();
    // System.out.println("format: " + key.getFormat());
    String keyString = new String(Base64.getEncoder().encode(keyByteArray), StandardCharsets.UTF_8);
    return keyString;
  }

  private PublicKey publicKeyFromString(String keyString) throws Exception {
    byte[] keyByteArray = keyString.getBytes(StandardCharsets.UTF_8);
    byte[] keyByteArrayDecoded = Base64.getDecoder().decode(keyByteArray);
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyByteArrayDecoded);
    KeyFactory kf = KeyFactory.getInstance("RSA");

    return kf.generatePublic(keySpec);
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


  public void decryptAESKey(int groupId, String AESKeyCiphertext) throws Exception {
    SecretKey decryptedAESKey = new SecretKeySpec(cipherDecrypt.doFinal(stringToBytes(AESKeyCiphertext)), "AES");
    AESKeys.put(Integer.valueOf(groupId), decryptedAESKey);
  }
  
  public List<String> createAndEncryptAESKey(int groupId, List<String> publicKeyList) throws Exception {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(256);
    SecretKey aesKey = keyGenerator.generateKey();
    AESKeys.put(Integer.valueOf(groupId), aesKey);

    List<String> encodedKeyList = new ArrayList<String>();
    for (String publicKeyString: publicKeyList) {
      Cipher cipherEncrypt = Cipher.getInstance(publicKeyAlgorithm);
      cipherEncrypt.init(Cipher.ENCRYPT_MODE, publicKeyFromString(publicKeyString));
      String encryptedAESKey = bytesToString(cipherEncrypt.doFinal(aesKey.getEncoded()));
      encodedKeyList.add(encryptedAESKey);
    }
    return encodedKeyList;
  }

  public String bytesToString(byte[] byteArray) {
    return new String(Base64.getEncoder().encode(byteArray), StandardCharsets.UTF_8);
  }

  public byte[] stringToBytes(String msgString) {
    return Base64.getDecoder().decode(msgString.getBytes(StandardCharsets.UTF_8));
  }

  public String encryptMessage(int groupId, String message) throws Exception {
    byte[] plaintextBytes = message.getBytes(StandardCharsets.UTF_8);
    SecretKey aesKey = AESKeys.get(Integer.valueOf(groupId));
    Cipher cipherEncrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipherEncrypt.init(Cipher.ENCRYPT_MODE, aesKey);
    byte[] cipherBytes = cipherEncrypt.doFinal(plaintextBytes);
    byte[] b64cipherBytes = Base64.getEncoder().encode(cipherBytes);
    // encoding doesn't matter because for b64 it's all the same
    return new String(b64cipherBytes, StandardCharsets.UTF_8);
  }

  public String decryptMessage(int groupId, String messageCiphertext) throws Exception {
    byte[] b64cipherBytes = messageCiphertext.getBytes(StandardCharsets.UTF_8);
    byte[] cipherBytes = Base64.getDecoder().decode(b64cipherBytes);
    SecretKey aesKey = AESKeys.get(Integer.valueOf(groupId));
    Cipher cipherDecrypt = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipherDecrypt.init(Cipher.DECRYPT_MODE, aesKey);
    byte[] plaintextBytes = cipherDecrypt.doFinal(cipherBytes);
    return new String(plaintextBytes, StandardCharsets.UTF_8);
  }

  // keysize is in number of bits 
  public Encryption(int keysize) throws Exception {
    this.keysize = keysize;
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(keysize);
    keyPair = keyPairGenerator.genKeyPair();
    cipherDecrypt = Cipher.getInstance(publicKeyAlgorithm);
    cipherDecrypt.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
  }
}
