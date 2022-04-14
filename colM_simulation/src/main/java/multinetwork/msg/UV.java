package multinetwork.msg;

import lombok.Getter;

import java.math.BigInteger;
import java.util.ArrayList;

public class UV {
    @Getter
    public ArrayList<BigInteger> u;
    @Getter
    public ArrayList<BigInteger> v;

    public UV(ArrayList<BigInteger> u, ArrayList<BigInteger> v) {
        this.u = u;
        this.v = v;
    }
}
