import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import javax.imageio.ImageIO;

/**
 * StegSign — Image Steganography + DSA Digital Signature
 *
 * This program demonstrates how to:
 *   1. Sign a message using DSA so we can prove who sent it and that it wasn't tampered with
 *   2. Hide that signed message inside a PNG image using LSB steganography
 *   3. Extract and verify the message on the receiver's side
 *
 * Run it with an "input.png" in the same directory. It will produce a "stego.png"
 * that looks identical to the original but carries a hidden signed message.
 *
 * Usage:
 *   javac *.java
 *   java MainApp
 */
public class MainApp {

    // Separator used to split the message from its DSA signature in the payload
    private static final String SIGN_DELIMITER = "::SIGN::";

    public static void main(String[] args) {
        try {
            runDemo();
        } catch (Exception e) {
            System.err.println("Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDemo() throws Exception {
        System.out.println("=== StegSign Demo ===\n");

        // ---- SENDER SIDE ----

        // Generate a DSA key pair for this session.
        // In a real app, you'd load these from a keystore or file instead.
        System.out.println("[Sender] Generating DSA key pair...");
        KeyPair keyPair = DSAUtil.generateKeyPair();
        System.out.println("[Sender] Keys ready.\n");

        String message = "Hello from the other side of the pixel.";
        System.out.println("[Sender] Message: \"" + message + "\"");

        // Sign the message so the receiver can verify it came from us
        String signature = DSAUtil.sign(message, keyPair.getPrivate());
        System.out.println("[Sender] Message signed.\n");

        // Bundle message + signature into a single payload string
        String payload = message + SIGN_DELIMITER + signature;

        // Load the cover image and embed the payload into it
        BufferedImage coverImage = loadImage("input.png");
        System.out.println("[Sender] Embedding payload into image...");
        Steganography.embedMessage(coverImage, payload);
        saveImage(coverImage, "stego.png");
        System.out.println("[Sender] Stego image saved as stego.png\n");

        // ---- RECEIVER SIDE ----

        System.out.println("[Receiver] Loading stego image...");
        BufferedImage stegoImage = loadImage("stego.png");

        // Extract the hidden payload
        String extracted = Steganography.extractMessage(stegoImage);

        // The delimiter separates the message from the signature
        String[] parts = extracted.split(SIGN_DELIMITER, 2);
        if (parts.length != 2) {
            System.err.println("[Receiver] Extraction failed — payload format not recognized.");
            return;
        }

        String extractedMessage = parts[0];
        String extractedSignature = parts[1];

        System.out.println("[Receiver] Extracted message: \"" + extractedMessage + "\"");

        // Verify authenticity — uses the sender's public key
        boolean valid = DSAUtil.verify(extractedMessage, extractedSignature, keyPair.getPublic());

        System.out.println("[Receiver] Signature valid: " + valid);
        if (valid) {
            System.out.println("[Receiver] Message is authentic and untampered. ✓");
        } else {
            System.out.println("[Receiver] WARNING: Signature mismatch — message may have been altered.");
        }
    }

    private static BufferedImage loadImage(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Image not found: " + path + " — make sure it's in the working directory.");
        }
        return ImageIO.read(file);
    }

    private static void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
    }
}
