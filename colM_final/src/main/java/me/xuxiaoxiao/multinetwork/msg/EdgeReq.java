package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

public class EdgeReq {
    @Getter
    private final String adminId;
    @Getter
    public Integer nodeId;

    public EdgeReq(String adminId, Integer nodeId) {
        this.adminId = adminId;
        this.nodeId = nodeId;
    }
}
