package src.util;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import src.message.Message;

public class ConsensusInstance {
    private int id;
    private ReentrantLock lock;

    // PROPOSER VALUES
    private long c_rnd;         // highest-numbered round (or ballot) the proposer has started
    private long c_val;         // value the proposer has picked for round c-rnd  
    private long v = -1;        // the value I want to propose
    private int count_1B = 0, count_2B = 0;
    private ArrayList<Message> messages_1B = new ArrayList<>();
    private boolean sent_2A, decided = false, executed = false;

    // ACCEPTOR VALUES
    private long rnd = 0;   // highest-numbered round the acceptor has participated
    private long v_rnd = 0; // highest-numbered round the acceptor has cast a vote
    private long v_val;     // value voted by the acceptor in round v-rnd, initially null

    // LEARNER VALUES
    private long decided_value = -1;

    // CONSTRUCTOR
    public ConsensusInstance(int id){
        this.id = id;
        this.lock = new ReentrantLock();
    }

    // COMMON FUNCTIONS
    @Override
    public boolean equals(Object o){return get_id() == ((ConsensusInstance) o).get_id();}
    public int get_id() {return this.id;}
    public void lock() {this.lock.lock();}
    public void unlock() {this.lock.unlock();}

    // PROPOSER FUNCTIONS
    public long get_c_rnd() {return c_rnd;}
    public long get_c_val() {return c_val;}
    public void set_c_val(long c_val) {this.c_val = c_val;}
    public long get_v() {return v;}
    public void set_v(long v) {this.v = v;}
    public int get_count_1B() {return count_1B;}
    public int get_count_2B() {return count_2B;}
    public ArrayList<Message> get_messages_1B() {return messages_1B;}
    public boolean has_quorum_1B() {return (count_1B >= PaxosEntity.ACCEPTORS_QUORUM);}
    public void add_message_2B(Message m) {count_2B++;}
    public boolean has_quorum_2B() {return (count_2B >= PaxosEntity.ACCEPTORS_QUORUM);}
    public boolean sent_2A() {return sent_2A;}
    public void set_sent_2A(boolean sent_2a) {sent_2A = sent_2a;}
    public boolean is_decided() {return decided;}
    public long get_decided_value() {return decided_value;}
    public void increment_c_rnd(int id_proposer){
        c_rnd++;
        while((c_rnd % PaxosEntity.NUM_OF_PROPOSERS) != id_proposer) c_rnd++;
        // when incrementing the round, reset instance
        c_val=0;
        count_1B = 0;
        count_2B = 0;
        messages_1B = new ArrayList<>();
        sent_2A = false;
        decided = false;
        executed = false;
        rnd = 0;
        v_rnd = 0;
        v_val=0;
        decided_value = -1;
    }
    public void add_message_1B(Message m) {
        messages_1B.add(m);
        count_1B++;
    }
    public long get_largest_v_rnd() {
        long k = 0;
        for(Message m1b : messages_1B){
          if(m1b.get_v_rnd() > k){
            k = m1b.get_v_rnd();
          }
        }
        return k;
    }
    public ArrayList<Message> get_set_V(long k) {
        ArrayList<Message> V = new ArrayList<>();
        for(Message m1b : messages_1B){
            if(m1b.get_v_rnd() == k){
                V.add(m1b);
            }
        }
        return V;
    }


    // ACCEPTOR FUNCTIONS
    public long get_rnd() {return rnd;}
    public void set_rnd(long rnd) {this.rnd = rnd;}
    public long get_v_rnd() {return v_rnd;}
    public void set_v_rnd(long v_rnd) {this.v_rnd = v_rnd;}
    public long get_v_val() {return v_val;}
    public void set_v_val(long v_val) {this.v_val = v_val;}


    // LEARNER FUNCTIONS
    public void set_decided_value(long decided_value) {
        this.decided_value = decided_value;
        decided = true;
    }
    public void execute(){
        //                              NOOP
        if(!executed && decided_value != -1){
            System.out.println(decided_value);
            executed = true;
        }
    }
}