package src.message;
import java.io.Serializable;

public class Message implements Serializable {
    private MessageTypes type;
    private int instance_id;    // the message belongs to this consensus instance
    private long c_rnd;         // highest-numbered round (or ballot) the proposer has started
    private long c_val;         // value the proposer has picked for round c-rnd
    private long rnd;           // highest-numbered round the acceptor has participated
    private long v_rnd;         // highest-numbered round the acceptor has cast a vote
    private long v_val;         // value voted by the acceptor in round v-rnd, initially null
    private long client_value;

    public MessageTypes get_type() {return type;}
    public int get_instance_id() {return instance_id;}
    public long get_client_value() {return client_value;}
    public long get_c_rnd() {return c_rnd;}
    public long get_c_val() {return c_val;}
    public long get_rnd() {return rnd;}
    public long get_v_rnd() {return v_rnd;}
    public long get_v_val() {return v_val;}
    public void set_instance_id(int instance_id) {this.instance_id = instance_id;}
    public void set_client_value(long client_value) {this.client_value = client_value;}
    public void set_type(MessageTypes type) {this.type = type;}
    public void set_c_rnd(long c_rnd) {this.c_rnd = c_rnd;}
    public void set_c_val(long c_val) {this.c_val = c_val;}
    public void set_rnd(long rnd) {this.rnd = rnd;}
    public void set_v_rnd(long v_rnd) {this.v_rnd = v_rnd;}
    public void set_v_val(long v_val) {this.v_val = v_val;}
}
