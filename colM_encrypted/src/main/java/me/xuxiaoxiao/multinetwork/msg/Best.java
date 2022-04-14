package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;

public class Best {
    @Getter
    public Integer best;

    public  Best(Integer best) {
        this.best = best;
    }
}
