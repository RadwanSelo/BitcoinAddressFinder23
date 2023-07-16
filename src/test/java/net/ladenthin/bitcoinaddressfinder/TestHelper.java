package net.ladenthin.bitcoinaddressfinder;

import com.google.common.hash.Hashing;
import net.ladenthin.bitcoinaddressfinder.configuration.CProducerOpenCL;
import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.hamcrest.Matchers;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TestHelper {

    private static final int GRID_NUM_BITS = 8;
    private static final int PRIVATE_KEY_MAX_BIT_LENGTH = 256;
    private static final int HEX_RADIX = 16;

    public static OpenCLContext createOpenCLContext(boolean chunkMode, int kernelMode) {
        new OpenCLPlatformAssume().assumeOpenCLLibraryLoadableAndOneOpenCL2_0OrGreaterDeviceAvailable();
        CProducerOpenCL producerOpenCL = new CProducerOpenCL();
        producerOpenCL.gridNumBits = GRID_NUM_BITS;
        producerOpenCL.chunkMode = chunkMode;
        producerOpenCL.kernelMode = kernelMode;
        OpenCLContext openCLContext = new OpenCLContext(producerOpenCL);

        try {
            openCLContext.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return openCLContext;
    }

    public static BigInteger[] generateRandomUncompressedPrivateKeys(int arraySize) {
        List<BigInteger> privateKeysList = new LinkedList<>();
        while (privateKeysList.size() < arraySize) {
            BigInteger candidate = KeyUtility.createSecret(PRIVATE_KEY_MAX_BIT_LENGTH, new SecureRandom());
            if (validBitcoinPrivateKey(candidate)) {
                privateKeysList.add(candidate);
            }
        }
        BigInteger[] privateKeysArray = new BigInteger[arraySize];
        for (int i = 0; i < arraySize; i++) {
            privateKeysArray[i] = privateKeysList.get(i);
        }
        return privateKeysArray;
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean validBitcoinPrivateKey(BigInteger candidate) {
        // Check if the private key is within the valid range
        BigInteger minPrivateKey = BigInteger.ONE;
        BigInteger maxPrivateKey = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16);
        if (!(candidate.compareTo(minPrivateKey) >= 0 && candidate.compareTo(maxPrivateKey) <= 0)) {
            return false;
        }
        if (candidate.toString(2).length() != 256) {
            return false;
        }
        return true;
    }

    public static BigInteger[] generateChunkOfPrivateKeysOutOfSinglePrivateKey(BigInteger singlePrivateKey, int arraySize) {
        BigInteger[] chunk = new BigInteger[arraySize];
        chunk[0] = singlePrivateKey;
        for (int i = 1; i < chunk.length; i++) {
            chunk[i] = bitwiseOrWithLast32Bits(singlePrivateKey, i);
        }
        return chunk;
    }

    /**
     * Simulates the or operation in OpenCL when using the chunk mode
     * <p>
     * This method will perform a bitwise OR-operation with the last 32 bits of a given BigInteger with the given value
     * <p>
     * <strong>The content of this method was generated with OpenAI/ChatGPT</strong>
     *
     * @param number The secret key as a BigInteger
     * @param value  The value which in OpenCL would be the global_id
     * @return The updated number
     */
    public static BigInteger bitwiseOrWithLast32Bits(BigInteger number, int value) {
        // Mask for the last 32 bits
        BigInteger mask = BigInteger.valueOf(0xFFFFFFFFL);

        // Extract the last 32 bits as a BigInteger
        BigInteger last32Bits = number.and(mask);

        // Perform bitwise OR operation with the given value
        BigInteger result = last32Bits.or(BigInteger.valueOf(value));

        // Update the last 32 bits in the number with the modified value
        BigInteger updatedNumber = number.and(mask.not()).or(result);

        return updatedNumber;
    }

    public static BigInteger[] createBigIntegerArrayFromHexStringArray(String[] hexStringArray) {
        BigInteger[] bigIntegerArray = new BigInteger[hexStringArray.length];
        for (int i = 0; i < hexStringArray.length; i++) {
            bigIntegerArray[i] = new BigInteger(hexStringArray[i], HEX_RADIX);
        }
        return bigIntegerArray;
    }

    public static BigInteger[] createBigIntegerArrayFromSingleHexString(String hexString) {
        return createBigIntegerArrayFromHexStringArray(new String[]{hexString});
    }

    public static String[] uncompressedPublicKeysHexStringArrayFromPrivateKeysArray(BigInteger[] privateKeysArray) {
        String[] uncompressedPublicKeysStringArray = new String[privateKeysArray.length];
        for (int i = 0; i < privateKeysArray.length; i++) {
            uncompressedPublicKeysStringArray[i] = uncompressedPublicKeyHexStringFromPrivateKey(privateKeysArray[i]);
        }
        return uncompressedPublicKeysStringArray;
    }

    public static String uncompressedPublicKeyHexStringFromPrivateKey(BigInteger privateKey) {
        return Hex.encodeHexString(uncompressedPublicKeyFromPrivateKey(privateKey));
    }

    public static byte[] uncompressedPublicKeyFromPrivateKey(BigInteger privateKey) {
        return ECKey.publicKeyFromPrivate(privateKey, false);
    }

    public static String hexStringFromBigInteger(BigInteger bigInteger) {
        return bigInteger.toString(HEX_RADIX);
    }

    public static String hexStringFromPublicKeyBytes(PublicKeyBytes publicKeyBytes) {
        return Hex.encodeHexString(publicKeyBytes.getUncompressed());
    }

    public static Map<String, String> createMapFromBigIntegerArrayAndPublicKeyBytesArray(BigInteger[] keyArray, PublicKeyBytes[] valueArray) {
        Map<String, String> map = new HashMap<>();
        if (keyArray.length != valueArray.length) {
            return null;
        }
        for (int i = 0; i < keyArray.length; i++) {
            String keyString = hexStringFromBigInteger(keyArray[i]);
            String valueString = hexStringFromPublicKeyBytes(valueArray[i]);
            map.put(keyString, valueString);
        }
        return map;
    }

    public static Map<String, String> createMapFromSecretAndPublicKeys(BigInteger[] keyArray, String[] valueArray) {
        Map<String, String> map = new HashMap<>();
        if (keyArray.length != valueArray.length) {
            return null;
        }
        for (int i = 0; i < keyArray.length; i++) {
            String keyString = hexStringFromBigInteger(keyArray[i]);
            String valueString = valueArray[i];
            map.put(keyString, valueString);
        }
        return map;
    }

    public static <K, V> ActualMap<K, V> assertThatKeyMap(Map<K, V> actualMap) {
        return new ActualMap<>(actualMap);
    }

    public static byte[] calculateSha256FromByteArray(byte[] digest) {
        return Hashing.sha256().hashBytes(digest).asBytes();
    }

    /**
     * Written by ChatGPT. Input was: "i need a java method to turn a hexString to a byte array".
     */
    public static byte[] byteArrayFromHexString(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    public static String hexStringFromByteArray(byte[] sha256HashResult) {
        return hexStringFromBigInteger(new BigInteger(sha256HashResult));
    }

    public static Map<String, String> createMapOfPublicKeyBytesAndSha256Bytes(PublicKeyBytes[] keyArray, Sha256Bytes[] valueArray) {
        Map<String, String> map = new HashMap<>();
        for (PublicKeyBytes key : keyArray) {
            String keyString = hexStringFromPublicKeyBytes(key);
            byte[] valueBytes = calculateSha256FromByteArray(key.getUncompressed());
            String valueString = hexStringFromByteArray(valueBytes);
            map.put(keyString, valueString);
        }
        return map;
    }

    /**
     * Generates the sha256 hash for each public key and stores both as hex Strings.
     *
     * @param publicKeys Array containing public keys as PublicKeyBytes elements.
     * @return map Containing the public keys and their sha256 both as hex Strings.
     */
    public static Map<String, String> createExpectedMapOfPublicKeyBytesAndSha256Bytes(PublicKeyBytes[] publicKeys) {
        Map<String, String> map = new HashMap<>();
        for (PublicKeyBytes publicKey : publicKeys) {
            String publicKeyHexString = hexStringFromPublicKeyBytes(publicKey);
            byte[] sha256Bytes = calculateSha256FromByteArray(publicKey.getUncompressed());
            String sha256HexString = hexStringFromByteArray(sha256Bytes);
            map.put(publicKeyHexString, sha256HexString);
        }
        return map;
    }

    /**
     * Generates the public key for each private key and stores both as hex Strings.
     *
     * @param privateKeys Array containing private keys as BigInteger elements.
     * @return map Containing the private keys and their public keys both as hex Strings.
     */
    public static Map<String, String> createExpectedMapOfPrivateKeysToPublicKeys(BigInteger[] privateKeys) {
        Map<String, String> map = new HashMap<>();
        for (BigInteger privateKey : privateKeys) {
            String privateKeyHexString = hexStringFromBigInteger(privateKey);
            String publicKeyHexString = uncompressedPublicKeyHexStringFromPrivateKey(privateKey);
            map.put(privateKeyHexString, publicKeyHexString);
        }
        return map;
    }

    /**
     * Map storing actual values for better test assertions. Compares size and if both are equal.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static class ActualMap<K, V> {

        private final Map<K, V> actualMap;

        private ActualMap(Map<K, V> actualMap) {
            assertThat(actualMap, Matchers.notNullValue());
            this.actualMap = actualMap;
        }

        public void isEqualTo(Map<K, V> expectedMap) {
            assertThat(expectedMap, Matchers.notNullValue());
            assertThat("None identical length of both maps!", actualMap.size(), is(equalTo(expectedMap.size())));
            Set<K> expectedKeys = expectedMap.keySet();
            for (K expectedKey : expectedKeys) {
                assertThat("Contains key", true, is(actualMap.containsKey(expectedKey)));
            }
            int i = 0;
            for (K expectedKey : expectedKeys) {
                String reason = "Current Element: " + i + "/" + (actualMap.size() - 1);
                final V actualValue = actualMap.get(expectedKey);
                final V expectedValue = expectedMap.get(expectedKey);
                reason += "\n\t  expectedKey = " + expectedKey.toString();
                reason += "\n\texpectedValue = " + expectedValue.toString();
                reason += "\n\t  actualValue = " + actualValue.toString();
                assertThat(reason, actualValue, is(equalTo(expectedValue)));
                System.out.println(reason);
                i++;
            }
        }
    }
}