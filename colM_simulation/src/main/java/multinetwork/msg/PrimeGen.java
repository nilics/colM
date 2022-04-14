package multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;

public class PrimeGen {
    @Getter
    public BigInteger p;
    @Getter
    public BigInteger g;

    public PrimeGen() {
        this.p = new BigInteger(String.valueOf(0));
        this.g = new BigInteger(String.valueOf(0));
    }

    public PrimeGen(BigInteger p, BigInteger g) {
        this.p = p;
        this.g = g;
    }
}
