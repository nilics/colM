package multinetwork.msg;

import lombok.Getter;

public class EdgeInfo {
    @Getter
    private final int node;
    @Getter
    private final int iEdgeNum;
    @Getter
    private final int oEdgeNum;

    public EdgeInfo(int node, int iEdgeNum, int oEdgeNum) {
        this.node = node;
        this.iEdgeNum = iEdgeNum;
        this.oEdgeNum = oEdgeNum;
    }
}
