// @formatter:off
/**
 * Copyright 2020 Bernard Ladenthin bernard.ladenthin@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
// @formatter:on
package net.ladenthin.bitcoinaddressfinder.staticaddresses;

import java.nio.ByteBuffer;
import net.ladenthin.bitcoinaddressfinder.ByteBufferUtility;
import net.ladenthin.bitcoinaddressfinder.KeyUtility;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;

public enum StaticP2SHAddress implements PublicAddress {

    /**
     * https://bitcoin.stackexchange.com/questions/62781/litecoin-constants-and-prefixes
     * https://privatekeys.pw/litecoin/address/M8T1B2Z97gVdvmfkQcAtYbEepune1tzGua
     * https://privatekeys.pw/bitcoin/address/3MSvaVbVFFLML86rt5eqgA9SvW23upaXdY
     */
    BitcoinAndLitecoinDeprecated("3MSvaVbVFFLML86rt5eqgA9SvW23upaXdY", "d8b83ad7bf8795b9ff61464fcf06f156c28e3e1f"),
    /**
     * P2TR (Pay-to-Taproot)
     * https://bitcoin.stackexchange.com/questions/110995/how-can-i-find-samples-for-p2tr-transactions-on-mainnet
     */
    BitcoinP2TR("bc1p5d7rjq7g6rdk2yhzks9smlaqtedr4dekq08ge8ztwac72sfr9rusxg3297", "a37c3903c8d0db6512e2b40b0dffa05e5a3ab73603ce8c9c4b7771e5412328f9"),
    // 
    /**
     * P2SH (Pay to Witness Script Hash) 
     * https://privatekeys.pw/bitcoin/address/bc1qp762gmkychywl4elnuyuwph68hqw0uc2jkzu3ax48zfjkskslpsq8p66gf
     */
    BitcoinP2WSH("bc1qp762gmkychywl4elnuyuwph68hqw0uc2jkzu3ax48zfjkskslpsq8p66gf", "0fb4a46ec4c5c8efd73f9f09c706fa3dc0e7f30a9585c8f4d538932b42d0f860"),
    /**
     * https://privatekeys.pw/dogecoin/address/A8c3xNz2mqsDLFwv5KL5fpH12QEwDaoTXo
     */
    Dogecoin("A8c3xNz2mqsDLFwv5KL5fpH12QEwDaoTXo", "b15b9098e14dc3c48b4f0a6ac548c66771d30f54"),
    /**
     * https://privatekeys.pw/dogecoin/address/9tp7THhBG3fNNFka1AJm95DU3E4x3pwQA3
     */
    DogecoinMultisig("9tp7THhBG3fNNFka1AJm95DU3E4x3pwQA3", "1a11b73ad3dd99d77da9f858cf323205a84dd5da"),
    /**
     * https://privatekeys.pw/dash/address/7dZe6LvtGPd2TLPpARzThqdF4YwAZvAKDv
     */
    Dash("7dZe6LvtGPd2TLPpARzThqdF4YwAZvAKDv", "7a5c931f83bb3a356c2dcf72b24e1bea461df587"),
    /**
     * https://privatekeys.pw/litecoin/address/M8T1B2Z97gVdvmfkQcAtYbEepune1tzGua
     */
    Litecoin("M8T1B2Z97gVdvmfkQcAtYbEepune1tzGua", "0605bfbd71b78f7e9fc815fe9cc90aaeb1d9a728"),
    /**
     * https://privatekeys.pw/zcash/address/t3JcMe1E5UkFsUtVb7k17eJwXX5FYUewMBy
     */
    Zcash("t3JcMe1E5UkFsUtVb7k17eJwXX5FYUewMBy", "00847bd140242bd1c9c19024bfe00a4acefbbb85");

    private final String publicAddress;
    private final String scriptHash;

    StaticP2SHAddress(String publicAddress, String scriptHash) {
        this.publicAddress = publicAddress;
        this.scriptHash = scriptHash;
    }

    @Override
    public String getPublicAddress() {
        return publicAddress;
    }

    public String getScriptHashAsHex() {
        return scriptHash;
    }

    public byte[] getScriptHash() {
        return Hex.decode(getScriptHashAsHex());
    }

    public ByteBuffer getScriptHashAsByteBuffer() {
        return new ByteBufferUtility(true).getByteBufferFromHex(getScriptHashAsHex());
    }

    public String getScriptHashAsBase58() {
        final NetworkParameters networkParameters = MainNetParams.get();
        KeyUtility keyUtility = new KeyUtility(networkParameters, new ByteBufferUtility(true));
        return keyUtility.toBase58(getScriptHash());
    }
}
