package net.ladenthin.bitcoinaddressfinder;

import org.jocl.CL;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Map;

import static net.ladenthin.bitcoinaddressfinder.TestHelper.assertThatKeyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests the publicKey and address calculation of an OpenClContext without considering the performance.
 */
public class OpenCLContextTest {

    public static final int CHUNK_SIZE = 256;

    private static final boolean CHUNK_MODE = true;
    private static final boolean NON_CHUNK_MODE = false;
    private static final String PRIVATE_KEY_HEX_STRING = "c297e4944f46f3b9f04cf4b3984f49bd4ee40dec33991066fa15cdb227933469";
    private static final String PUBLIC_KEY_HEX_STRING = "045f399867ee13c5ac525259f036c90f455b11d667acfcdfc36791288547633611e8416a53aea83bd55691a5721775a581bd1e8e09dd3db4021a6f6daebdbcc9da";
    private static final String ADDRESS_HEX_STRING = "1MYEKfUnUpw6714iJ7Tm4Ndrqzpx2ufWpa";

    private static final String ERROR_CODE_SUCCESS = CL.stringFor_errorCode(CL.CL_SUCCESS);

    @Test
    public void test_generateSinglePublicKey_specificPrivateKey() {
        // arrange
        BigInteger[] privateKeysArray = TestHelper.createBigIntegerArrayFromSingleHexString(PRIVATE_KEY_HEX_STRING);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(CHUNK_MODE, OpenCLContext.GEN_PUBLIC_KEYS_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(privateKeysArray);
        PublicKeyBytes[] publicKeysResult = openCLGridResult.getPublicKeyBytes();
        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        String resultPublicKeyAsHexString = TestHelper.hexStringFromPublicKeyBytes(publicKeysResult[0]);
        assertThat(resultPublicKeyAsHexString, is(equalTo(PUBLIC_KEY_HEX_STRING)));
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }

    @Test
    public void test_generateSinglePublicKey_randomPrivateKey() {
        //arrange
        BigInteger[] privateKeysArray = TestHelper.generateRandomUncompressedPrivateKeys(1);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(CHUNK_MODE, OpenCLContext.GEN_PUBLIC_KEYS_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(privateKeysArray);
        PublicKeyBytes[] publicKeysResult = openCLGridResult.getPublicKeyBytes();

        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        String resultPublicKeyAsHexString = TestHelper.hexStringFromPublicKeyBytes(publicKeysResult[0]);
        String expectedPublicKeyAsHexString = TestHelper.uncompressedPublicKeyHexStringFromPrivateKey(privateKeysArray[0]);
        assertThat(resultPublicKeyAsHexString, is(equalTo(expectedPublicKeyAsHexString)));
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }

    @Test
    public void test_generate256PublicKeys_specificSinglePrivateKey_chunkMode() {
        //arrange
        BigInteger[] privateKeysArray = TestHelper.createBigIntegerArrayFromSingleHexString(PRIVATE_KEY_HEX_STRING);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(CHUNK_MODE, OpenCLContext.GEN_PUBLIC_KEYS_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(privateKeysArray);
        PublicKeyBytes[] publicKeysResult = openCLGridResult.getPublicKeyBytes();

        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        BigInteger[] privateKeysChunk = TestHelper.generateChunkOfPrivateKeysOutOfSinglePrivateKey(privateKeysArray[0], CHUNK_SIZE);
        Map<String, String> resultKeysMap = TestHelper.createMapFromBigIntegerArrayAndPublicKeyBytesArray(privateKeysChunk, publicKeysResult);
        String[] expectedPublicKeysAsHexStringArray = TestHelper.uncompressedPublicKeysHexStringArrayFromPrivateKeysArray(privateKeysChunk);
        Map<String, String> expectedKeysMap = TestHelper.createMapFromSecretAndPublicKeys(privateKeysChunk, expectedPublicKeysAsHexStringArray);
        assertThatKeyMap(resultKeysMap).isEqualTo(expectedKeysMap);
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }

    @Test
    public void test_generate256PublicKeys_randomSinglePrivateKey_chunkMode() {
        //arrange
        BigInteger[] privateKeysArray = TestHelper.generateRandomUncompressedPrivateKeys(1);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(CHUNK_MODE, OpenCLContext.GEN_PUBLIC_KEYS_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(privateKeysArray);
        PublicKeyBytes[] publicKeysResult = openCLGridResult.getPublicKeyBytes();

        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        BigInteger[] privateKeysChunk = TestHelper.generateChunkOfPrivateKeysOutOfSinglePrivateKey(privateKeysArray[0], CHUNK_SIZE);
        Map<String, String> resultKeysMap = TestHelper.createMapFromBigIntegerArrayAndPublicKeyBytesArray(privateKeysChunk, publicKeysResult);
        String[] expectedPublicKeysAsHexStringArray = TestHelper.uncompressedPublicKeysHexStringArrayFromPrivateKeysArray(privateKeysChunk);
        Map<String, String> expectedKeysMap = TestHelper.createMapFromSecretAndPublicKeys(privateKeysChunk, expectedPublicKeysAsHexStringArray);
        assertThatKeyMap(resultKeysMap).isEqualTo(expectedKeysMap);
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }

    @Test
    public void test_generate256PublicKeys_random256PrivateKeys_nonChunkMode() {
        //arrange
        BigInteger[] randomPrivateKeys = TestHelper.generateRandomUncompressedPrivateKeys(CHUNK_SIZE);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(NON_CHUNK_MODE, OpenCLContext.GEN_PUBLIC_KEYS_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(randomPrivateKeys);
        PublicKeyBytes[] publicKeysResult = openCLGridResult.getPublicKeyBytes();

        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        Map<String, String> resultKeysMap = TestHelper.createMapFromBigIntegerArrayAndPublicKeyBytesArray(randomPrivateKeys, publicKeysResult);
        String[] expectedPublicKeysAsHexStringArray = TestHelper.uncompressedPublicKeysHexStringArrayFromPrivateKeysArray(randomPrivateKeys);
        Map<String, String> expectedKeysMap = TestHelper.createMapFromSecretAndPublicKeys(randomPrivateKeys, expectedPublicKeysAsHexStringArray);
        assertThatKeyMap(resultKeysMap).isEqualTo(expectedKeysMap);
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }
        // arrange
        BigInteger[] privateKeysArray = TestHelper.createBigIntegerArrayFromSingleHexString(PRIVATE_KEY_HEX_STRING);
        OpenCLContext openCLContext = TestHelper.createOpenCLContext(CHUNK_MODE, OpenCLContext.GEN_ADDRESSES_MODE);

        // act
        OpenCLGridResult openCLGridResult = openCLContext.createResult(privateKeysArray);
        AddressBytes[] addressBytesResult = openCLGridResult.getAddressBytes();

        // cleanup
        openCLContext.release();
        openCLGridResult.freeResult();

        // assert
        String resultAddressAsHexString = TestHelper.hexStringFromAddressBytes(addressBytesResult[0]);
        assertThat(resultAddressAsHexString, is(equalTo(ADDRESS_HEX_STRING)));
        assertThat(openCLContext.getErrorCodeString(), is(equalTo(ERROR_CODE_SUCCESS)));
    }

    @Ignore
    @Test
    public void test_generateSingleAddress_randomSinglePrivateKey() {
        // TODO write test
    }

    @Ignore
    @Test
    public void test_generate256Addresses_random256PrivateKeys() {
        // TODO write test
    }
}