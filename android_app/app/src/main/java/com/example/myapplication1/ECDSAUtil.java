package com.example.myapplication1;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * ECCDSA加签验签工具类
 * @author Administrator
 *
 */
public class ECDSAUtil {

    public static final String PRIVATE_KEY = "308187020100301306072a8648ce3d020106082a8648ce3d030107046d306b020101042047bdd3c6e418ef046f2ff6752c50a87af99441e6291c20810abb1ea6823f07b6a14403420004ef0a48639c8c82e524e90c9dc187b42e03c0f19252a2a7fcabc6dbaa264422721ae9eb982ed81748ea9c8bd71b39a6c95dce76e962cc160546c56eb522834b94";
    public static final String PUBLIC_KEY = "3059301306072a8648ce3d020106082a8648ce3d03010703420004ef0a48639c8c82e524e90c9dc187b42e03c0f19252a2a7fcabc6dbaa264422721ae9eb982ed81748ea9c8bd71b39a6c95dce76e962cc160546c56eb522834b94";
    private static final String SIGNALGORITHMS = "SHA256withECDSA";
    private static final String ALGORITHM = "EC";

    /**
     * 加签
     * @param data 数据
     * @return
     */
    public static byte[] signECDSA( byte[] data) {
        byte[] result = {0x00};
        try {
            //执行签名
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(HexUtil.decodeHex(PRIVATE_KEY.toCharArray()));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(SIGNALGORITHMS);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 验签
     * @param signed 签名
     * @param data 数据
     * @return
     */
    public static boolean verifyECDSA( byte[] signed, byte[] data) {
        try {
            //验证签名
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(HexUtil.decodeHex(PUBLIC_KEY.toCharArray()));
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance(SIGNALGORITHMS);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(signed);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 生成密钥对
     * @return
     * @throws Exception
     */
    public static KeyPair getKeyPair() throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }


}
