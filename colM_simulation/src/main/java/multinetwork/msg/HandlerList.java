package multinetwork.msg;

import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public class HandlerList {
    @Getter
    private final List<String> handlerList = new LinkedList<String>();
}
