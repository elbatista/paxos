import java.util.HashMap;

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
    String host = getConfig().get("acceptors").split(":")[0];
    int port = Integer.valueOf(getConfig().get("acceptors").split(":")[1]);
    System.out.println("Hello from Acceptor with id " + getId() + " config host " + host + " port " + port);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    switch(m.getType()){
      case Phase1A: message1A(m); break;
      case Phase2A: message2A(m); break;
      default: return;
    }    
  }

  private void message1A(Message m) {
    System.out.println("Acceptor: message from proposer - 1A");

    // DO STUFF

    m.setType(MessageTypes.Phase1B);
    sendToProposers(m);
  }

  private void message2A(Message m) {
    System.out.println("Acceptor: message from proposer - 2A");

    // DO STUFF

    m.setType(MessageTypes.Phase2B);
    sendToProposers(m);
  }

  private void sendToProposers(Message m) {
    String [] hostPort = getConfig().get("proposers").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}