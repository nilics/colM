package me.xuxiaoxiao.multinetwork.test;

import java.math.BigInteger;
public class SecurityKey {
	private BigInteger key;
	private BigInteger p;
	private BigInteger g;
	public SecurityKey(BigInteger key,BigInteger p,BigInteger g) {
		this.key=key;
		this.p=p;
		this.g=g;
	}
	public BigInteger getKey() {
		return key;
	}
	public void setKey(BigInteger key) {
		this.key = key;
	}
	public BigInteger getP() {
		return p;
	}
	public void setP(BigInteger p) {
		this.p = p;
	}
	public BigInteger getG() {
		return g;
	}
	public void setG(BigInteger g) {
		this.g = g;
	}
}
