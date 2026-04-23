import java.security.*;
import java.util.Base64;

/**
 * Handles all the DSA key generation, signing, and verification logic.
 *
 * Quick note on why DSA over RSA here: DSA is specifically designed for
 * signatures (not encryption), so it's a cleaner fit for this use case.
 * We're using 2048-bit keys — that's the current sweet spot between
 * performance and security.
 */
public class DSAUtil {

    private static final String ALGORITHM = "DSA";
    private static final String SIGN_ALGORITHM = "SHA256withDSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generates a fresh DSA key pair.
     * Call this once on the sender side and hold onto both keys.
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Signs a message with the given private key.
     * Returns a Base64 string so it's easy to embed as plain text inside the image.
     *
     * @param data       the message you want to sign
     * @param privateKey the sender's private key
     * @return Base64-encoded signature
     */
    public static String sign(String data, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signer = Signature.getInstance(SIGN_ALGORITHM);
        signer.initSign(privateKey);
        signer.update(data.getBytes());

        byte[] rawSignature = signer.sign();
        return Base64.getEncoder().encodeToString(rawSignature);
    }

    /**
     * Verifies a signature against the original message using the sender's public key.
     * If even one character of the message was changed after signing, this returns false.
     *
     * @param data         the message that was extracted from the image
     * @param signatureB64 the Base64 signature extracted from the image
     * @param publicKey    the sender's public key
     * @return true if the message is authentic and untampered
     */
    public static boolean verify(String data, String signatureB64, PublicKey publicKey) throws GeneralSecurityException {
        Signature verifier = Signature.getInstance(SIGN_ALGORITHM);
        verifier.initVerify(publicKey);
        verifier.update(data.getBytes());

        byte[] rawSignature = Base64.getDecoder().decode(signatureB64);
        return verifier.verify(rawSignature);
    }
}
