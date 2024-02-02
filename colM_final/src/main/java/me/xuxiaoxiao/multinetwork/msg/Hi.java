package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;

public class Hi extends BigInteger {
    @Getter
    public BigInteger hi;

    public  Hi(BigInteger hi) {
        super("0");
        this.hi = hi;
    }
}
