package src.message;
import java.io.Serializable;

public class Message implements Serializable {
    private MessageTypes type;
    private long c_rnd; // highest-numbered round (or ballot) the proposer has started
    private long c_val; // value the proposer has picked for round c-rnd
    private long rnd;   // highest-numbered round the acceptor has participated
    private long v_rnd; // highest-numbered round the acceptor has cast a vote
    private long v_val; // value voted by the acceptor in round v-rnd, initially null

    private long clientValue;

    public long getClientValue() {
        return clientValue;
    }
    public void setClientValue(long clientValue) {
        this.clientValue = clientValue;
    }
    public MessageTypes getType() {
        return type;
    }
    public void setType(MessageTypes type) {
        this.type = type;
    }
    public long getC_rnd() {
        return c_rnd;
    }
    public void setC_rnd(long c_rnd) {
        this.c_rnd = c_rnd;
    }
    public long getC_val() {
        return c_val;
    }
    public void setC_val(long c_val) {
        this.c_val = c_val;
    }
    public long getRnd() {
        return rnd;
    }
    public void setRnd(long rnd) {
        this.rnd = rnd;
    }
    public long getV_rnd() {
        return v_rnd;
    }
    public void setV_rnd(long v_rnd) {
        this.v_rnd = v_rnd;
    }
    public long getV_val() {
        return v_val;
    }
    public void setV_val(long v_val) {
        this.v_val = v_val;
    }
}
