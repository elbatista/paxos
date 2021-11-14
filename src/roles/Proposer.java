package src.roles;
import java.util.ArrayList;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Proposer extends PaxosEntity {

  private long c_rnd; // highest-numbered round (or ballot) the proposer has started
  private long c_val; // value the proposer has picked for round c-rnd  
  private long v;     // the value I want to propose
  private int count1B = 0, count2B = 0;
  private ArrayList<Message> messages1B = new ArrayList<>();
  
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

  private void inc_c_rnd(){
    c_rnd++;
    while((getC_rnd() % NUM_OF_PROPOSERS) != getId()){
      c_rnd++;
    }
  }

  private void inc_c_rnd(long target){
    while(getC_rnd() <= target){
      c_rnd++;
      while((getC_rnd() % NUM_OF_PROPOSERS) != getId()){
        c_rnd++;
      }
    }
  }

  public Proposer(int id, HashMap<String, String> config){
    super(id, config);
    String conf = getConfig().get("proposers");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Proposer " + getId() + "; config: " + conf);
    setC_rnd(0);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    switch(m.getType()){
      case CLIENT: messageFromClient(m); break;
      case PHASE_1B: message1B(m); break;
      case PHASE_2B: message2B(m); break;
      default: return;
    }    
  }

  private void message1B(Message m) {
    // upon receiving (PHASE 1B, rnd, v-rnd, v-val) from Qa such that c-rnd = rnd
    //    k ← largest v-rnd value received
    //    V ← set of (v-rnd,v-val) received with v-rnd = k
    //    if k = 0 then let c-val be v
    //    else c-val ← the only v-val in V
    //    send (PHASE 2A, c-rnd, c-val) to acceptors
    if(m.getRnd() == getC_rnd()){
      messages1B.add(m);
      count1B++;
      if(count1B == ACCEPTORS_QUORUM){
        System.out.println("Message 1B from quorum for round "+getC_rnd());

        // k ← largest v-rnd value received
        long k = 0;
        for(Message m1b : messages1B){
          if(m1b.getV_rnd() > k){
            k = m1b.getV_rnd();
          }
        }
        System.out.println("K = " + k);

        // if k = 0 then let c-val be v
        if(k == 0){
          m.setC_val(v);
        }
        else {
          // V ← set of (v-rnd,v-val) received with v-rnd = k
          ArrayList<Message> V = new ArrayList<>();
          for(Message m1b : messages1B){
            if(m1b.getV_rnd() == k){
              V.add(m1b);
            }
          }
          // else c-val ← the only v-val in V
          long the_only_v_val = V.get(0).getV_val();
          for(int i=1; i< V.size(); i++){
            if(V.get(i).getV_val() != the_only_v_val){
              System.err.println("Error: v-rnd is not unique in V for round " + getC_rnd());
              System.exit(0);
            }
            the_only_v_val = V.get(i).getV_val();
          }
          System.out.println("the_only_v_val = " + the_only_v_val);
          m.setC_val(the_only_v_val);
        }
       
        m.setType(MessageTypes.PHASE_2A);
        sendToAcceptors(m);
      }
    }
  }

  private void message2B(Message m) {
    // upon receiving (PHASE 2B, v-rnd, v-val) from Qa
    //    if for all received messages: v-rnd = c-rnd then
    //      send (DECISION, v-val) to learners
    if(m.getV_rnd() == getC_rnd()){
      count2B++;
      if(count2B == ACCEPTORS_QUORUM){
        System.out.println("Message 2B from quorum for round "+getC_rnd()+". Sending decision value: " + m.getV_val());
        // v-val is already set in message.v_val
        m.setType(MessageTypes.DECIDE);
        sendToLearners(m);
      }
    }
  }

  private void messageFromClient(Message m) {
    System.out.println("Message from client with value: "+m.getClientValue());
    // To propose value v:
    //    increase c-rnd to an arbitrary unique value
    //    send (PHASE 1A, c-rnd) to acceptors

    // store in v the value that I want to propose (the value from client)
    v = m.getClientValue();
    inc_c_rnd();
    m.setType(MessageTypes.PHASE_1A);
    m.setC_rnd(getC_rnd());
    sendToAcceptors(m);
  }

  private void sendToLearners(Message m) {
    String [] hostPort = getConfig().get("learners").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  private void sendToAcceptors(Message m) {
    String [] hostPort = getConfig().get("acceptors").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}