package src.util;
import java.util.ArrayList;

import src.message.Message;

public class ConsensusInstance {
    private long id;

    // PROPOSERS' VALUES

    //private long c_rnd; // highest-numbered round (or ballot) the proposer has started
    //private long c_val; // value the proposer has picked for round c-rnd  
    private int phase1B_Count = 0;
    private int phase2B_Count = 0;
    private boolean sentPhase2A = false;
    private ArrayList<Message> phase1B_Messages = new ArrayList<>();
    private ArrayList<Message> phase2B_Messages = new ArrayList<>();


    // ACCEPTORS' VALUES



    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public boolean sentPhase2A() {
        return sentPhase2A;
    }
    public void setSentPhase2A() {
        this.sentPhase2A = true;
    }
    public int getPhase1B_Count() {
        return phase1B_Count;
    }
    public int getPhase2B_Count() {
        return phase2B_Count;
    }
    public ArrayList<Message> getPhase1B_Messages() {
        return phase1B_Messages;
    }
    public void setPhase1B_Messages(ArrayList<Message> phase1b_Messages) {
        phase1B_Messages = phase1b_Messages;
    }
    public ArrayList<Message> getPhase2B_Messages() {
        return phase2B_Messages;
    }
    public void setPhase2B_Messages(ArrayList<Message> phase2b_Messages) {
        phase2B_Messages = phase2b_Messages;
    }
    public void incrementPhase1B(){
        phase1B_Count++;
    }
    public void incrementPhase2B(){
        phase2B_Count++;        
    }
}
