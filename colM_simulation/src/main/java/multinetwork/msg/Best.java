package multinetwork.msg;

import lombok.Getter;

public class Best {
    @Getter
    public Integer best;

    public Best(Integer best) {
        this.best = best;
    }
}
