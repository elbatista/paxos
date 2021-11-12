import java.util.HashMap;

public class Proposer extends PaxosEntity {

  private long c_rnd; // highest-numbered round (or ballot) the proposer has started
  private long c_val; // value the proposer has picked for round c-rnd
  
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

  public Proposer(int id, HashMap<String, String> config){
    super(id, config);
    String host = getConfig().get("proposers").split(":")[0];
    int port = Integer.valueOf(getConfig().get("proposers").split(":")[1]);
    System.out.println("Hello from Proposer with id " + getId() + " config host " + host + " port " + port);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    
    switch(m.getType()){
      case NULL: messageFromClient(m); break;
      case Phase1B: message1B(m); break;
      case Phase2B: message2B(m); break;
      default: return;
    }    
  }

  private void message1B(Message m) {
    System.out.println("Proposer: message from acceptors - 1B");
  }

  private void message2B(Message m) {
    System.out.println("Proposer: message from acceptors - 2B");
  }

  private void messageFromClient(Message m) {
    System.out.println("Proposer: message from client");
    m.setType(MessageTypes.Phase1A);
    sendToAcceptors(m);
  }

  private void sendToAcceptors(Message m) {
    String [] hostPort = getConfig().get("acceptors").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}