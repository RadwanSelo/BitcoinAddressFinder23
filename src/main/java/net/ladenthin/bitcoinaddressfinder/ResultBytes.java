package net.ladenthin.bitcoinaddressfinder;

import java.util.Arrays;

/**
 * Data structure to store bytewise buffered result written by OpenCL kernel.
 */
public class ResultBytes {

    public static final int NUM_BYTES_PRIVATE_KEY = 32;
    public static final int NUM_BYTES_PUBLIC_KEY = 65;
    public static final int NUM_BYTES_SHA256 = 32;
    public static final int NUM_BYTES_RIPEMD160 = 20;
    public static final int NUM_BYTES_TOTAL_UNTIL_RIPEMD160 = NUM_BYTES_PRIVATE_KEY + NUM_BYTES_PUBLIC_KEY + NUM_BYTES_SHA256 + NUM_BYTES_RIPEMD160;

    private final byte[] privateKeyBytes;
    private final byte[] publicKeyBytes;
    private final byte[] firstSha256Bytes;
    private final byte[] ripemd160Bytes;

    /**
     * Constructor for storing all results in a data structure. It will retrieve all results from the given result buffer content.
     *
     * @param resultBytes The complete result buffer as a byte array
     */
    public ResultBytes(byte[] resultBytes) {
        privateKeyBytes = new byte[NUM_BYTES_PRIVATE_KEY];
        System.arraycopy(resultBytes, 0, privateKeyBytes, 0, NUM_BYTES_PRIVATE_KEY);
        publicKeyBytes = new byte[NUM_BYTES_PUBLIC_KEY];
        System.arraycopy(resultBytes, NUM_BYTES_PRIVATE_KEY, publicKeyBytes, 0, NUM_BYTES_PUBLIC_KEY);
        firstSha256Bytes = new byte[NUM_BYTES_SHA256];
        System.arraycopy(resultBytes, NUM_BYTES_PRIVATE_KEY + NUM_BYTES_PUBLIC_KEY, firstSha256Bytes, 0, NUM_BYTES_SHA256);
        ripemd160Bytes = new byte[NUM_BYTES_RIPEMD160];
        System.arraycopy(resultBytes, NUM_BYTES_PRIVATE_KEY + NUM_BYTES_PUBLIC_KEY + NUM_BYTES_SHA256, ripemd160Bytes, 0, NUM_BYTES_RIPEMD160);
    }

    /**
     * Constructor for storing all results in a data structure.
     *
     * @param privateKey  The private key as a byte array
     * @param publicKey   The public key as a byte array
     * @param firstSha256 The first SHA-256 hash as a byte array
     * @param ripemd160   The RIPEMD-160 hash as a byte array
     */
    public ResultBytes(byte[] privateKey, byte[] publicKey, byte[] firstSha256, byte[] ripemd160) {
        this.privateKeyBytes = privateKey;
        this.publicKeyBytes = publicKey;
        this.firstSha256Bytes = firstSha256;
        this.ripemd160Bytes = ripemd160;
    }

    /**
     * The private key.
     *
     * @return private key as byte array
     */
    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    /**
     * The uncompressed public key with parity.
     *
     * @return uncompressed public key as byte array
     */
    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }

    /**
     * The SHA-256 hash of the uncompressed public key.
     *
     * @return first SHA-256 hash as byte array
     */
    public byte[] getFirstSha256BytesBytes() {
        return firstSha256Bytes;
    }

    /**
     * The RIPEMD-160 hash of the previous SHA-256 hash.
     *
     * @return RIPEMD-160 hash as byte array
     */
    public byte[] getRipemd160BytesBytes() {
        return ripemd160Bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultBytes that = (ResultBytes) o;
        return Arrays.equals(privateKeyBytes, that.privateKeyBytes) && Arrays.equals(publicKeyBytes, that.publicKeyBytes) && Arrays.equals(firstSha256Bytes, that.firstSha256Bytes) && Arrays.equals(ripemd160Bytes, that.ripemd160Bytes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(privateKeyBytes);
        result = 31 * result + Arrays.hashCode(publicKeyBytes);
        result = 31 * result + Arrays.hashCode(firstSha256Bytes);
        result = 31 * result + Arrays.hashCode(ripemd160Bytes);
        return result;
    }
}