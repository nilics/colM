package multinetwork.msg;

import lombok.Getter;

public class EdgeFragment {
    @Getter
    private final int nodeid;//边请求信息：要发送的服务端ip+端口号，节点的id，
    @Getter
    private final double iFrag; //内部边数
    @Getter
    private final double oFrag;//外部边数

    public EdgeFragment(int nodeid, double iFrag, double oFrag) {
        this.nodeid = nodeid;
        this.iFrag = iFrag;
        this.oFrag = oFrag;
    }
}
