package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Acceptor extends PaxosEntity {

  private long rnd;   // highest-numbered round the acceptor has participated
  private long v_rnd; // highest-numbered round the acceptor has cast a vote
  private long v_val; // value voted by the acceptor in round v-rnd, initially null
  
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

  public Acceptor(int id, HashMap<String, String> config){
    super(id, config);
    String conf = getConfig().get("acceptors");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Acceptor " + getId() + "; config: " + conf);
    setRnd(0);
    setV_rnd(0);
    setV_val(0);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    switch(m.getType()){
      case PHASE_1A: message1A(m); break;
      case PHASE_2A: message2A(m); break;
      default: return;
    }    
  }

  private void message1A(Message m) {
    // upon receiving (PHASE 1A, c-rnd) from proposer
    //    if c-rnd > rnd then
    //      rnd ← c-rnd
    //      send (PHASE 1B, rnd, v-rnd, v-val) to proposer
    if(m.getC_rnd() > getRnd()){
      System.out.println("Message 1A: " + m.getC_rnd() +", "+ getRnd());
      setRnd(m.getC_rnd());
      m.setType(MessageTypes.PHASE_1B);
      m.setRnd(getRnd());
      m.setV_rnd(getV_rnd());
      m.setV_val(getV_val());
      sendToProposers(m);
    }
  }

  private void message2A(Message m) {
    // upon receiving (PHASE 2A, c-rnd, c-val) from proposer
    //    if c-rnd ≥ rnd then
    //        v-rnd ← c-rnd
    //        v-val ← c-val
    //        send (PHASE 2B, v-rnd, v-val) to proposer
    if(m.getC_rnd() >= getRnd()){
      System.out.println("Message 2A");
      setV_rnd(m.getC_rnd());
      setV_val(m.getC_val());
      m.setType(MessageTypes.PHASE_2B);
      m.setV_rnd(getV_rnd());
      m.setV_val(getV_val());
      sendToProposers(m);
    }
  }

  private void sendToProposers(Message m) {
    String [] hostPort = getConfig().get("proposers").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}