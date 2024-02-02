package me.xuxiaoxiao.multinetwork.msg;

import lombok.Getter;

public class HandlerId {
    @Getter
    private final String id;

    public HandlerId(String id) {
        this.id = id;
    }
}
