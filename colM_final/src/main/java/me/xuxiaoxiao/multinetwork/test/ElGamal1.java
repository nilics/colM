package me.xuxiaoxiao.multinetwork.test;

import me.xuxiaoxiao.multinetwork.EIGAMAL.ChangeBase;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.spec.DHParameterSpec;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;

public class ElGamal1 {


	public static final String KEY_ALGORITHM = "ElGamal";
	/**
	 * 密钥长度，DH算法的默认密钥长度是1024
	 * 密钥长度必须是8的倍数，在160到16384位之间
	 */
	private static final int KEY_SIZE = 160;
	private static final String PUBLIC_KEY = "ElGamalPublicKey"; //公钥
	private static final String PRIVATE_KEY = "ElGamalPrivateKey"; //私钥
	private static boolean True;

	public static BigInteger p=null;//p=2q+1
	public static BigInteger q=null;
	public static BigInteger g=null;

	public  static BigInteger GetPrimordialRoot() {
		BigInteger PrimordialRoot=new BigInteger("2");
		while(PrimordialRoot.compareTo(p.subtract(new BigInteger("1")))==-1) {
			//判断是否为本源根，具体证明见附件
			if(PrimordialRoot.modPow(new BigInteger("2"), p).equals(new
					BigInteger("1"))||PrimordialRoot.modPow(q, p).equals(new BigInteger("1")))
				PrimordialRoot=PrimordialRoot.add(new BigInteger("1"));
			else
				break;
		}
		if(PrimordialRoot.compareTo(p.subtract(new BigInteger("1")))==-1)
			return PrimordialRoot;
		else
			return null;
	}
	public static void main(String[] args) throws Exception {

//		Security.addProvider(new BouncyCastleProvider()); //加入对BouncyCastle支持
//		AlgorithmParameterGenerator apg = AlgorithmParameterGenerator.getInstance(KEY_ALGORITHM);
//		apg.init(KEY_SIZE);//初始化参数生成器
//		AlgorithmParameters params = apg.generateParameters();//生成算法参数
//		//构建参数材料
//		DHParameterSpec elParams = (DHParameterSpec) params.getParameterSpec(DHParameterSpec.class);
//		BigInteger p = elParams.getP();
//		BigInteger g = elParams.getG();




		while(true) {
			//Runtime.getInstance().exec("cls");
			//获得512位长度的大素数q
			q=BigInteger.probablePrime(512, new Random());
			//计算p=2q
			p=q.multiply(new BigInteger("2")).add(new BigInteger("1"));
			//判断p是不是素数
			if(p.isProbablePrime(20))
				break;
		}

		//找出Zp的生成元g
		g=GetPrimordialRoot();

		p = new BigInteger("23333549391663695385288013284442107401803411405323301196491683562780320236703944716560126054468785184158255912186304412517433339996290968387400967503917967");
		g = new BigInteger("5");
		System.out.println("p:"+p.toString());
		System.out.println("g:"+g.toString());

		//3. 生成自己的私钥xi

		StringBuffer key=new StringBuffer();
		//随机生成256位的密钥
		for(int i=0;i<256;i++) {
			Random ran=new Random();
			int x=ran.nextInt(10);
			if(x>5)
				key=key.append(1);
			else
				key=key.append(0);
		}
		//构造私钥钥

		//BigInteger privateKey0=new BigInteger(key.toString(),2);
		BigInteger privateKey0=new BigInteger("104147137590674773303301157249396851417376887624771913623292646091799108959712");
		BigInteger publicKey0=g.modPow(privateKey0, p);

		System.out.println("privateKey0:"+privateKey0.toString());
		System.out.println("publicKey0:"+publicKey0.toString());

		StringBuffer key1=new StringBuffer();
		//随机生成256位的密钥
		for(int i=0;i<256;i++) {
			Random ran=new Random();
			int x=ran.nextInt(10);
			if(x>5)
				key1=key1.append(1);
			else
				key1=key1.append(0);
		}
		//构造私钥钥
		//BigInteger privateKey1=new BigInteger(key1.toString(),2);
		BigInteger privateKey1=new BigInteger("60444020554458661893774510757299200793330537947330226664443730403415209518092");
		BigInteger publicKey1=g.modPow(privateKey1, p);

		System.out.println("privateKey1:"+privateKey1.toString());
		System.out.println("publicKey1:"+publicKey1.toString());


		StringBuffer key2=new StringBuffer();
		//随机生成256位的密钥
		for(int i=0;i<256;i++) {
			Random ran=new Random();
			int x=ran.nextInt(10);
			if(x>5)
				key2=key2.append(1);
			else
				key2=key2.append(0);
		}
		//构造私钥钥
		//BigInteger privateKey2=new BigInteger(key2.toString(),2);
		BigInteger privateKey2=new BigInteger("50669131580201177324564390059597775222107011831404498509370495252661256454216");
		BigInteger publicKey2=g.modPow(privateKey2, p);

		System.out.println("privateKey2:"+privateKey2.toString());
		System.out.println("publicKey2:"+publicKey2.toString());


		//BigInteger publicKey=publicKey2.multiply(publicKey0).multiply(publicKey1);
		BigInteger publicKey=publicKey2.multiply(publicKey0).multiply(publicKey1);
		System.out.println("publicKey1:"+publicKey.toString());
		publicKey=publicKey.mod(p);
		System.out.println("publicKey mod p:"+publicKey.toString());

		Random ran=new  Random();
		//k<p-1.我们这里取他的子集
		int k=364135542;//ran.nextInt(1000000000);
		//计算c1
		BigInteger C10=g.modPow(new BigInteger(k+""), p);
		//BigInteger M0=ChangeBase.ecode("1");
		BigInteger M0=new BigInteger("1");
		BigInteger C20=((publicKey).modPow(new BigInteger(k+""), p)).multiply(M0).mod(p);

		System.out.println("k:"+k);
		System.out.println("C10:"+C10.toString());
		System.out.println("C20:"+C20.toString());

		//k<p-1.我们这里取他的子集
		int k1=565210633;//ran.nextInt(1000000000);
		//计算c1
		BigInteger C11=g.modPow(new BigInteger(k1+""), p);
		//BigInteger M1=ChangeBase.ecode("1");
		BigInteger M1=new BigInteger("2");
		BigInteger C21=((publicKey).modPow(new BigInteger(k1+""), p)).multiply(M1).mod(p);

		System.out.println("k1:"+k1);
		System.out.println("C11:"+C11.toString());
		System.out.println("C21:"+C21.toString());

		//k<p-1.我们这里取他的子集
		int k2=245691773;//ran.nextInt(1000000000);
		//计算c1
		BigInteger C12=g.modPow(new BigInteger(k2+""), p);
		//BigInteger M2=ChangeBase.ecode("1");
		BigInteger M2=new BigInteger("3");
		BigInteger C22=(( publicKey).modPow(new BigInteger(k2+""), p)).multiply(M2).mod(p);

		System.out.println("k2:"+k2);
		System.out.println("C12:"+C12.toString());
		System.out.println("C22:"+C22.toString());

		BigInteger C1=C12.multiply(C11).multiply(C10);
		BigInteger C2=C22.multiply(C21).multiply(C20);

		System.out.println("C1:"+C1.toString()+"C1*c2"+C11.multiply(C10).toString());
		System.out.println("C2:"+C2.toString()+"C21*c20"+C21.multiply(C20));

		BigInteger w0=C1.modPow(privateKey0,p);
		BigInteger w1=C1.modPow(privateKey1,p).multiply(w0);
		BigInteger w2=C1.modPow(privateKey2,p).multiply(w1);

		System.out.println("w0:"+w0.toString());
		System.out.println("w1:"+w1.toString());
		System.out.println("w2:"+w2.toString());

		BigInteger m=C2.multiply(w2.modInverse(p)).mod(p);

		//BigInteger publicKey=
		//System.out.println("\n public:"+publicKey.toString());

		//BigInteger M=new BigInteger(String.valueOf("123"));
		//System.out.println("\n M:"+M.toString());
        //miwen

//		Random ran=new  Random();
//		//k<p-1.我们这里取他的子集
//		int k=ran.nextInt(1000000000);
//		//int k=896779712;
//		//计算c1
//		BigInteger C1=g.modPow(new BigInteger(k+""), p);
//		BigInteger M=ChangeBase.ecode("456");
//		//System.out.println("\n  C1:"+ C1);
//		//计算c2
//		//System.out.println("\n  M:"+ M);
//		BigInteger C2=(((BigInteger) publicKey).modPow(new BigInteger(k+""), p)).multiply(M).mod(p);
//		//System.out.println("\n  C2:"+ C2);
//		//BigInteger C3=C2




//		StringBuffer C=new StringBuffer();
//
//		C.append(C1+":"+C2+",");
//		String ttt=Base64.getEncoder().encodeToString(C.toString().getBytes());
//
////调试到C2.
//		String ciphertext= new String(Base64.getDecoder().decode(ttt),"utf8");
//
//		String[] CS=ciphertext.split(",");
//		List<Byte> Plaintext=new ArrayList<>();
//		String C1_temp=CS[0];
//
//		//获得每块的C1和C2
//		C1=new BigInteger(C1_temp.split(":")[0]);
//		C2=new BigInteger(C1_temp.split(":")[1]);
//
//		//System.out.println("\n  C3:"+ C3);
//		//解密
//		//BigInteger m=C2.multiply(C1.modPow(((BigInteger) privateKey).negate(), p)).mod(p);
//
//		//bi1.modInverse(bi2);
//		BigInteger m=C2.multiply(C1.modPow(((BigInteger) privateKey).negate(), p)).mod(p);
		System.out.println("\n  解密后 m:"+ m);

		System.out.println("\n  解密后 m:"+ m.toString());

		BigInteger[] bigs= {m};
		System.out.println("m:"+ ChangeBase.decode(bigs));

	}
}
