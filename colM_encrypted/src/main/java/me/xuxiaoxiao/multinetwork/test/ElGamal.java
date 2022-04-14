

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


import javax.crypto.Cipher;
import javax.crypto.spec.DHParameterSpec;

//import org.apache.commons.codec.binary.Base64;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
/**
 * 非对称加密算法——ELGamal算法
 * 对于：“Illegal key size or default parameters”异常，是因为美国的出口限制，Sun通过权限文件（local_policy.jar、US_export_policy.jar）做了相应限制。
 * Java 7 无政策限制文件：http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html,
 * 下载后得到UnlimitedJCEPolicyJDK7.zip，解压替换%JAVA_HOME%/jre/lib/security的两个文件即可
 * @author gaopengfei
 *
 */
public class ElGamal {

	private static String src = "object-oriented！@#*5"; // 需要加密的原始字符串

	public static void main(String[] args) throws Exception {
		System.out.println("初始字符串：" + src);
		bouncyCastleELGamal();
	}

	/**
	 * Bouncy Castle实现ELGamal，这种算法和RSA算法的区别是只能公钥加密，私钥解密
	 * */
	private static void bouncyCastleELGamal() throws Exception{

		//Security.addProvider(new BouncyCastleProvider());//加入对Bouncy Castle的支持

		//1.初始化发送方密钥
		AlgorithmParameterGenerator algorithmParameterGenerator = AlgorithmParameterGenerator.getInstance("ELGamal");
		algorithmParameterGenerator.init(256);//初始化参数生成器
		AlgorithmParameters algorithmParameters = algorithmParameterGenerator.generateParameters();//生成算法参数
		DHParameterSpec dhParameterSpec = (DHParameterSpec)algorithmParameters.getParameterSpec(DHParameterSpec.class);//构建参数材料
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ELGamal");//实例化密钥对生成器
		//初始化密钥对生成器
		keyPairGenerator.initialize(dhParameterSpec, new SecureRandom());
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		//公钥和私钥
		PublicKey elGamalPublicKey = keyPair.getPublic();
		PrivateKey elGamalPrivateKey = keyPair.getPrivate();
		//System.out.println("ELGamal公钥：" + Base64.encodeBase64String(elGamalPublicKey.getEncoded()));
		//System.out.println("ELGamal私钥：" + Base64.encodeBase64String(elGamalPrivateKey.getEncoded()));

		//2.加密【公钥加密】
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(elGamalPublicKey.getEncoded());
		KeyFactory keyFactory = KeyFactory.getInstance("ELGamal");
		elGamalPublicKey = (PublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
		Cipher cipher = Cipher.getInstance("ELGamal","BC");
		cipher.init(Cipher.ENCRYPT_MODE, elGamalPublicKey);
		byte[] result = cipher.doFinal(src.getBytes());
		//System.out.println("ELGamal公钥加密：" + Base64.encodeBase64String(result));

		//3.解密【私钥解密】
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(elGamalPrivateKey.getEncoded());
		keyFactory = KeyFactory.getInstance("ELGamal");
		elGamalPrivateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		cipher.init(Cipher.DECRYPT_MODE, elGamalPrivateKey);
		result = cipher.doFinal(result);
		System.out.println("ELGamal私钥解密：" + new String(result));

//        //加密【私钥加密】
//        pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(elGamalPrivateKey.getEncoded());
//        keyFactory = KeyFactory.getInstance("ELGamal");
//        elGamalPrivateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
//        cipher.init(Cipher.ENCRYPT_MODE, elGamalPrivateKey);
//        result = cipher.doFinal(src.getBytes());
//        System.out.println("ELGamal私钥加密：" + Base64.encodeBase64String(result));
//
//        //解密【公钥解密】
//        x509EncodedKeySpec = new X509EncodedKeySpec(elGamalPublicKey.getEncoded());
//        keyFactory = KeyFactory.getInstance("ELGamal");
//        elGamalPublicKey = keyFactory.generatePublic(x509EncodedKeySpec);
//        cipher.init(Cipher.DECRYPT_MODE, elGamalPublicKey);
//        result = cipher.doFinal(result);
//        System.out.println("ELGamal解密：" + new String(result));
	}

}