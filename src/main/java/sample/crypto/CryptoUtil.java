/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 - 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package sample.crypto;

import com.github.jtendermint.crypto.ByteUtil;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import sample.classes.CustomPublicKey;
import sample.classes.IPFSFile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sample.classes.Transaction;

public abstract class CryptoUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    //ASYM
    private static int RSA_KEY_LENGTH = 4096;
    private static String ALGORITHM_NAME = "RSA";
    private static String PADDING_SCHEME = "PKCS1Padding";
    private static String MODE_OF_OPERATION = "ECB";
    private static String ASYM = ALGORITHM_NAME + "/" + MODE_OF_OPERATION + "/" + PADDING_SCHEME;
    //SYM
    private static int AES_KEY_SIZE = 128;
    private static int IV_SIZE = 96;
    private static int TAG_BIT_LENGTH = 128;
    private static String ALGO_TRANSFORMATION_STRING = "AES/GCM/NoPadding";

    public static KeyPair generateAsymKeypair() throws Exception{
        KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance(ALGORITHM_NAME);
        rsaKeyGen.initialize(RSA_KEY_LENGTH);
        return rsaKeyGen.generateKeyPair();
    }

    public static byte[] generateSymKey() throws Exception{

        byte iv[] = new byte[IV_SIZE];
        SecureRandom secRandom = new SecureRandom();
        secRandom.nextBytes(iv);

        byte random[] = new byte[16];
        secRandom = new SecureRandom();
        secRandom.nextBytes(random);

        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(AES_KEY_SIZE);
        SecretKey aesKey = keygen.generateKey();

        return SerializationUtils.serialize(new FullSymKey(iv, random, aesKey));
    }

    public static byte[] encryptAsym (PublicKey pubKey, byte[] data) throws Exception{
        Cipher c = Cipher.getInstance(ASYM);
        c.init(Cipher.ENCRYPT_MODE, pubKey);
        return c.doFinal(data);
    }

    public static byte[] decryptAsym (PrivateKey privKey, byte[] data) throws Exception{
        Cipher c = Cipher.getInstance(ASYM);
        c.init(Cipher.DECRYPT_MODE, privKey);
        return c.doFinal(data);
    }

    public static byte[] encryptSym (byte [] byteKey, byte[] data) throws Exception{
        FullSymKey key = SerializationUtils.deserialize(byteKey);
        Cipher c = Cipher.getInstance(ALGO_TRANSFORMATION_STRING);
        GCMParameterSpec gcm = new GCMParameterSpec(TAG_BIT_LENGTH, key.getIv());
        c.init(Cipher.ENCRYPT_MODE, key.getKey(), gcm);
        c.updateAAD(key.getRandom());
        byte [] result = c.doFinal(data);
        return result;
    }

    public static byte[] decryptSym (byte [] byteKey, byte[] data) throws Exception{
        FullSymKey key = SerializationUtils.deserialize(byteKey);
        Cipher c = Cipher.getInstance(ALGO_TRANSFORMATION_STRING);
        GCMParameterSpec gcm = new GCMParameterSpec(TAG_BIT_LENGTH, key.getIv());
        c.init(Cipher.DECRYPT_MODE, key.getKey(), gcm);
        c.updateAAD(key.getRandom());
        byte [] result = c.doFinal(data);
        return result;
    }

    public static String sha256hash(byte[] bytesToHash){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return ByteUtil.toString00(md.digest(bytesToHash));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "0x000";
        }
    }

    public static void saveAsymKeypair(KeyPair keys){
        RSAPrivateKey priv = (RSAPrivateKey) keys.getPrivate();
        RSAPublicKey pub = (RSAPublicKey) keys.getPublic();
        PEMFile privFile = new PEMFile(priv, "RSA PRIVATE KEY");
        PEMFile pubFile = new PEMFile(pub, "RSA PUBLIC KEY");
        try {
            privFile.write(".id_rsa");
            pubFile.write(".id.rsa.pub");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static KeyPair loadAsymKeypair() {

        KeyFactory factory = null;
        try {
            factory = KeyFactory.getInstance(ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        KeyPair keys = null;
        try {
            keys = new KeyPair(generatePublicKey(factory, ".id.rsa.pub"), generatePrivateKey(factory, ".id_rsa"));
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keys;

    }
    private static PrivateKey generatePrivateKey(KeyFactory factory, String filename) throws InvalidKeySpecException, IOException {
        PEMFile pemFile = new PEMFile(filename);
        byte[] content = pemFile.getPemObject().getContent();
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        return factory.generatePrivate(privKeySpec);
    }

    private static PublicKey generatePublicKey(KeyFactory factory, String filename) throws InvalidKeySpecException, IOException {
        PEMFile pemFile = new PEMFile(filename);
        byte[] content = pemFile.getPemObject().getContent();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(content);
        return factory.generatePublic(pubKeySpec);
    }

    public static String publicKeyToString(PublicKey pub){
        Gson gson = new Gson();
        RSAPublicKey helper = (RSAPublicKey) pub;
        CustomPublicKey custom = new CustomPublicKey(helper.getModulus(), helper.getPublicExponent());
        return Base64.getEncoder().encodeToString(gson.toJson(custom).getBytes());
    }

    public static String stringFromIpfsFile(Transaction file){
        Gson gson = new Gson();
        return Base64.getEncoder().encodeToString(gson.toJson(file).getBytes());
    }

    public static IPFSFile ipfsFileFromString(String fileString){
        byte[] decoded = Base64.getDecoder().decode(fileString);
        byte[] base64Decoded = DatatypeConverter.parseBase64Binary(new String(decoded));
        Gson gson = new Gson();
        Transaction trans = gson.fromJson(new String(base64Decoded) ,Transaction.class);
        return trans.getIpfsFile();
    }

    public static PublicKey publicKeyFromString(String key){

        byte[] decoded = Base64.getDecoder().decode(key);
        byte[] base64Decoded = DatatypeConverter.parseBase64Binary(new String(decoded));
        Gson gson = new Gson();
        CustomPublicKey custom = gson.fromJson(new String(base64Decoded) , CustomPublicKey.class);
        PublicKey pubKey = null;

        try {
            pubKey = KeyFactory.getInstance(ALGORITHM_NAME).generatePublic(new RSAPublicKeySpec(custom.getMod(),custom.getExp()));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return pubKey;
    }

}
