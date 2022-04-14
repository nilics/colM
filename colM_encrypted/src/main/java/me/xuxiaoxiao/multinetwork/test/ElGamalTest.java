package me.xuxiaoxiao.multinetwork.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;

public class ElGamalTest {

	static BigInteger p=null;//p=2q+1
	static BigInteger q=null;
	static BigInteger g=new BigInteger("2");
	//获取Zp的生成元（p的本源根）g
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
	//初始化系统，生成大素数p，p的本源根g。
	public static void init() {
		System.out.println("系统正在初始化");
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
		System.out.println("系统初始化完成");
	}
	//生成公私钥对
	public static void KeyGenerator()throws Exception {
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
		p=new BigInteger("18485970372875148713512604615072395657114565837555283173472221381648046133213546180943026853875535855656357781691187124356847055120560758739590179401129099");
		g=new BigInteger(String.valueOf(2));



		//BigInteger privateKey=new BigInteger("101546224158298624506173781296098695697393158482097589064397070496168311521639");
		//构造私钥钥
		BigInteger temps=new BigInteger("101546224158298624506173781296098695697393158482097589064397070496168311521639");
		SecurityKey privateKey=new SecurityKey(temps,p,g);
		System.out.println("\n new BigInteger(key.toString(),2):"+new BigInteger(key.toString(),2));
		//构造公钥
		SecurityKey publicKey=new SecurityKey(g.modPow(privateKey.getKey(), p),p,g);
		System.out.println("\n g.modPow(privateKey.getKey(), p):"+g.modPow(privateKey.getKey(), p));

		//把公钥和私钥分别存储在publicKey.key和privateKey.key文件里
		String path=new File("").getCanonicalPath();
		out(path+"\\privateKey.key",privateKey.getKey()+","+privateKey.getP()+","+privateKey.getG());
		out(path+"\\publicKey.key",publicKey.getKey()+","+publicKey.getP()+","+publicKey.getG());
		System.out.println("你的私钥存放在："+path+"\\privateKey.key");
		System.out.println("你的公钥存放在："+path+"\\publicKey.key");
	}
	//加密算法
	public static String encrypt(SecurityKey Publickey,String data){
		try {
			//BigInteger M= me.xuxiaoxiao.multinetwork.EIGAMAL.ChangeBase.ecode(data);
			BigInteger M=new BigInteger("123");
			StringBuffer C=new StringBuffer();
			Random ran=new  Random();
			//k<p-1.我们这里取他的子集
			//int k=ran.nextInt(1000000000);
			int k=896779712;

			p=new BigInteger("18485970372875148713512604615072395657114565837555283173472221381648046133213546180943026853875535855656357781691187124356847055120560758739590179401129099");
			g=new BigInteger(String.valueOf(2));



			BigInteger privateKey=new BigInteger("101546224158298624506173781296098695697393158482097589064397070496168311521639");

			//计算c1
			BigInteger C1=g.modPow(new BigInteger(k+""), Publickey.getP());
			//计算c2
			BigInteger C2=Publickey.getKey().modPow(new BigInteger(k+""), p).multiply(M).mod(p);
			System.out.println("\n C2:"+C1);
			System.out.println("\n C3:"+C2);



//			//计算c1
//			BigInteger C1=Publickey.getG().modPow(new BigInteger(k+""), Publickey.getP());
//			//计算c2
//			BigInteger C2=Publickey.getKey().modPow(new BigInteger(k+""), Publickey.getP()).multiply(M).mod(Publickey.getP());
			C.append(C1+":"+C2+",");
			System.out.println("\n k:"+k);
			System.out.println("\n C:"+C);
			return Base64.getEncoder().encodeToString(C.toString().getBytes());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	//解密算法
	public static String decrypt(SecurityKey Privatekey,String data) {
		try {
			String ciphertext= new String(Base64.getDecoder().decode(data),"utf8");
			//分解密文
			String[] CS=ciphertext.split(",");
			List<Byte> Plaintext=new ArrayList<>();
			String C=CS[0];

			//获得每块的C1和C2
			BigInteger C1=new BigInteger(C.split(":")[0]);
			BigInteger C2=new BigInteger(C.split(":")[1]);

			System.out.println("\n nei-C1:"+C1);
			System.out.println("\n nei-C2:"+C2);


			//在这里不能简单的用除法，而是乘以逆元
			BigInteger m=C2.multiply(C1.modPow(Privatekey.getKey().negate(), Privatekey.getP())).mod(Privatekey.getP());

			System.out.println("\n nei-m:"+m.toString());
			BigInteger[] bigs= {m};
			return me.xuxiaoxiao.multinetwork.EIGAMAL.ChangeBase.decode(bigs);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	//封装输出流
	public static void out(String path,String val) {
		try {
			val=Base64.getEncoder().encodeToString(val.getBytes("utf8"));
			FileWriter fw=new FileWriter(path);
			BufferedWriter bw=new BufferedWriter(fw);
			PrintWriter outs=new PrintWriter(bw);
			outs.println(val);
			outs.flush();
			outs.close();
		}
		catch(Exception ex) {

			ex.printStackTrace();
		}
	}
	//从文件中读取公私钥
	public static SecurityKey read(String path){
		SecurityKey sk=null;
		try {
			File f=new File(path);
			FileReader fr=new FileReader(f);
			BufferedReader br=new BufferedReader(fr);
			String line=null;
			StringBuffer sb=new StringBuffer();
			while((line=br.readLine())!=null)
			{
				byte[] b= Base64.getDecoder().decode(line);
				String[] key=new String(b,"utf8").split(",");
				if(key.length<3)
					throw new Exception("文件错误");
				sk=new SecurityKey(new BigInteger(key[0]),new BigInteger(key[1]),new BigInteger(key[2]));
			}
			br.close();
			return sk;

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return sk;
	}



	public static void main(String[] args) {

		try {
			//执行系统初始化生成p，g
			init();
			//构造公私钥
			KeyGenerator();
			System.out.println("\nP:"+p);
			System.out.println("\ng:"+g);
			Scanner sc=new Scanner(System.in);
			String str ="";
			sc.useDelimiter("\n");
			System.out.print("请输入公钥地址按回车结束：");
			if(sc.hasNext())
			{
				str=sc.next();
			}
			//获取公钥
			SecurityKey publicKey= read(str.substring(0,str.length()-1));

			System.out.print("请输入需要加密的文字按回车结束:");
			if(sc.hasNext())
			{
				str=sc.next();
			}	    //加密明文
			String c=encrypt(publicKey,str);

			System.out.println("加密结果:"+c);
			System.out.print("请输入私钥地址按回车可对密文数据进行解密:");
			if(sc.hasNext())
			{
				str=sc.next();
			}
			//获得私钥
			SecurityKey privateKey= read(str.substring(0,str.length()-1));
			//解密密文
			String m=decrypt(privateKey,c);
			System.out.println("解密明文："+m);
			sc.close();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}

	}
}
