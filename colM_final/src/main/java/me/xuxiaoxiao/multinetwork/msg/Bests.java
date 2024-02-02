package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.util.List;

public class Bests {
    @Getter
    public List<Integer> bests;

    public Bests(List<Integer> bests) {
        this.bests = bests;
    }
}
