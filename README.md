# StegSign

Hide a digitally signed message inside a PNG image — secretly.

This project combines two things that individually aren't unusual, but together make something interesting:

- **LSB Steganography** — hide data inside image pixels without visibly changing the image
- **DSA Digital Signatures** — prove that a message came from a specific sender and wasn't modified in transit

The result: an image that looks completely normal, but carries a hidden message that can be verified as authentic by whoever receives it.

---

## How it works

### Sender

1. Write your message
2. Sign it with your DSA private key
3. Bundle `message + signature` into a single payload string
4. Embed it into any PNG by flipping the least significant bit of pixel values
5. Send the (visually unchanged) image to the receiver

### Receiver

1. Open the stego image
2. Extract the payload by reading the LSBs of pixels
3. Split out the message and signature
4. Verify the signature using the sender's public key

If the image was tampered with after embedding — or the message was changed — the signature check fails. That's the whole point.

---

## Project structure

```
StegSign/
├── src/
│   ├── DSAUtil.java        ← key generation, signing, verification
│   ├── Steganography.java  ← LSB embed/extract logic
│   └── MainApp.java        ← puts it all together
└── README.md
```

---

## Running it

You'll need Java 11+ and an `input.png` in the same directory as your compiled classes.

```bash
# Compile
javac src/*.java -d out/

# Run (from the out/ directory, with input.png present)
cd out
cp ../input.png .
java MainApp
```

You'll get a `stego.png` output and a console log showing the full embed/extract/verify cycle.

---

## A few things to know

**Always use PNG.** JPEG recompresses pixel data on save, which wipes out the embedded bits. PNG is lossless, so the bits survive intact.

**Image size limits the payload.** Each pixel can hide exactly 1 bit. A 500×500 image gives you ~31 KB of capacity. DSA signatures at 2048-bit keys produce Base64 strings around 400 characters, so the overhead is modest — a reasonable-sized image handles it easily.

**This demo keeps the key pair in memory.** In production, you'd serialize the public key and share it with receivers out-of-band. The private key would live in a keystore, not in source code.

---

## What's happening under the hood

Every pixel in a PNG has an RGB value stored as a 32-bit integer. The lowest bit of that integer — when flipped — changes the color by exactly 1 out of 255 steps. That's imperceptible to human vision. We use that bit as a carrier: one bit per pixel, enough to hide a full signed message in a normal-sized image.

The message length is stored in the first 32 pixels (as a 32-bit integer, one bit per pixel), and the actual message bits follow right after. On extraction, we read the length first, then read that many bytes, and hand back the full payload.

---

## Possible next steps

- Encrypt the message before embedding (AES-GCM makes sense here)
- Load keys from a file/keystore instead of generating fresh ones each run
- Add a simple CLI with flags for `--embed` and `--extract`
- Try DCT-domain steganography for stronger resistance to detection
