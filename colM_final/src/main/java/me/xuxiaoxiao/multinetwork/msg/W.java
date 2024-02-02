package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;
import java.util.ArrayList;

public class W{
    @Getter
    public ArrayList<BigInteger> w;

    public  W(ArrayList<BigInteger> w) {
        this.w = w;
    }
    //public  setW(ArrayList<BigInteger> w) {
//        this.w = w;
//    }
}
