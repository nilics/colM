package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

public class End {
    @Getter
    public Integer flag;

    public  End(Integer flag) {
        this.flag = flag;
    }
}
