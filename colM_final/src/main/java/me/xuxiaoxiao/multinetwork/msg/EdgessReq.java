package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

import java.util.List;

public class EdgessReq {
    @Getter
    private final String adminId;
    @Getter
    public List<Integer> nodesId;

    public EdgessReq(String adminId, List<Integer> nodesId) {
        this.adminId = adminId;
        this.nodesId = nodesId;
    }
}
