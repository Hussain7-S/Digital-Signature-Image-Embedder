import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

/**
 * LSB (Least Significant Bit) steganography — hide a text payload inside a PNG image.
 *
 * The idea is simple: each pixel's color value is stored as an integer, and
 * flipping just the last bit causes a color change of ±1, which is completely
 * invisible to the human eye. We use that last bit as a carrier for our data.
 *
 * Layout of embedded bits inside the image:
 *   Bits  0–31  : 32-bit integer = total byte length of the message
 *   Bits 32–end : the actual message bytes, one bit per pixel
 *
 * Important: always use PNG. JPEG re-compresses the pixels and will destroy
 * the hidden data completely.
 */
public class Steganography {

    // How many pixels we use just to store the message length
    private static final int LENGTH_HEADER_BITS = 32;

    /**
     * Embeds the payload string into the image by modifying LSBs of pixel values.
     * The image is modified in-place — save it yourself after calling this.
     *
     * @param image   a loaded BufferedImage (must be PNG when saved)
     * @param payload the text to hide (message + signature combined)
     * @throws IllegalArgumentException if the image is too small for the payload
     */
    public static void embedMessage(BufferedImage image, String payload) {
        byte[] msgBytes = payload.getBytes(StandardCharsets.UTF_8);
        int msgLen = msgBytes.length;

        int totalBitsNeeded = LENGTH_HEADER_BITS + (msgLen * 8);
        int availableBits = image.getWidth() * image.getHeight();

        if (totalBitsNeeded > availableBits) {
            throw new IllegalArgumentException(
                "Image too small. Need " + totalBitsNeeded + " bits, but only have " + availableBits
            );
        }

        // Step 1: write the message length into the first 32 pixels
        for (int i = 0; i < LENGTH_HEADER_BITS; i++) {
            int bit = (msgLen >> i) & 1;
            writeBitToPixel(image, i, bit);
        }

        // Step 2: write each bit of the message, offset by the header
        for (int i = 0; i < msgLen * 8; i++) {
            int bit = (msgBytes[i / 8] >> (i % 8)) & 1;
            writeBitToPixel(image, i + LENGTH_HEADER_BITS, bit);
        }
    }

    /**
     * Reads the LSBs from the image to reconstruct the hidden payload string.
     *
     * @param image the stego image to extract from
     * @return the original payload string (message + "::SIGN::" + signature)
     */
    public static String extractMessage(BufferedImage image) {
        // Step 1: reconstruct message length from first 32 pixels
        int msgLen = 0;
        for (int i = 0; i < LENGTH_HEADER_BITS; i++) {
            int bit = readBitFromPixel(image, i);
            msgLen |= (bit << i);
        }

        // Step 2: reconstruct message bytes
        byte[] msgBytes = new byte[msgLen];
        for (int i = 0; i < msgLen * 8; i++) {
            int bit = readBitFromPixel(image, i + LENGTH_HEADER_BITS);
            msgBytes[i / 8] |= (byte) (bit << (i % 8));
        }

        return new String(msgBytes, StandardCharsets.UTF_8);
    }

    // --- private helpers ---

    /**
     * Converts a flat bit index to (x, y) coordinates and flips
     * the LSB of that pixel's RGB value to match our desired bit.
     */
    private static void writeBitToPixel(BufferedImage image, int bitIndex, int bit) {
        int x = bitIndex % image.getWidth();
        int y = bitIndex / image.getWidth();

        int rgb = image.getRGB(x, y);
        // clear the last bit, then set it to our desired value
        rgb = (rgb & 0xFFFFFFFE) | (bit & 1);
        image.setRGB(x, y, rgb);
    }

    /**
     * Reads the LSB from the pixel at the given flat bit index.
     */
    private static int readBitFromPixel(BufferedImage image, int bitIndex) {
        int x = bitIndex % image.getWidth();
        int y = bitIndex / image.getWidth();

        int rgb = image.getRGB(x, y);
        return rgb & 1;
    }
}
