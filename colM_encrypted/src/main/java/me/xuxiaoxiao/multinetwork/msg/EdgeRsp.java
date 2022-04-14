package me.xuxiaoxiao.multinetwork.msg;


import lombok.Getter;
import lombok.Setter;

public class EdgeRsp {


    @Getter
    @Setter
    private double inFrag;

    @Getter
    @Setter
    private double outFrag;

    public EdgeRsp(double inFrag,double outFrag) {
        this.inFrag = inFrag;
        this.inFrag = inFrag;
    }
}
