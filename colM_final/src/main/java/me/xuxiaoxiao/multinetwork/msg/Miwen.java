package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;

public class Miwen{
    @Getter
    public BigInteger v;
    public BigInteger u;

    public  Miwen(BigInteger u,BigInteger v) {
        this.v = v;
        this.u = u;
    }


}
