package me.xuxiaoxiao.multinetwork.EIGAMAL;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
//压缩密文长度
public class ChangeBase {
	//把传进来的明文编译成一个大数。
	public static BigInteger ecode(String m) throws Exception{
		byte[] ms=m.getBytes("utf8");


		StringBuffer sb=new StringBuffer("11111111");
		for(int j=0;j<ms.length;j++)
		{
			if(j<ms.length) {
				String big=	new BigInteger(Byte.toUnsignedInt(ms[j])+"").toString(2);
				while(big.length()<8)
					big=0+big;
				sb.append(big);
			}
		}
		return new BigInteger(sb+"",2);


	}
	//把传进来的大数编译成明文
	public static String decode(BigInteger[] datas) throws Exception{
		List<Byte> bss=new ArrayList<>();
		for(int j=0;j<datas.length;j++) {
			BigInteger data=datas[j];
			String  data0b=data.toString(2);
			for(int i=1;i<data0b.length()/8;i++)
			{
				byte b=(byte)Integer.parseInt(new BigInteger(data0b.substring(i*8, i*8+8),2)+"");
				bss.add(b);
			}
		}
		Byte[] bs=new Byte[bss.size()];
		byte[] bs1=new byte[bss.size()];
		bss.toArray(bs);
		for(int i=0;i<bs.length;i++)
		{
			bs1[i]=bs[i];
		}
		return new String(bs1,"utf8");
	}

}
