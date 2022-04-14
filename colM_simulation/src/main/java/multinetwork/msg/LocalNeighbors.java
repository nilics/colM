package multinetwork.msg;

import lombok.Getter;

import java.util.HashSet;

public class LocalNeighbors {
    @Getter
    public HashSet<Integer> Neighbors;
    public Integer flag;

    public LocalNeighbors(HashSet<Integer> Neighbors) {

        this.Neighbors = Neighbors;
        this.flag=0;

    }
}
