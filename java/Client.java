import java.util.HashMap;

public class Client extends PaxosEntity {

  public Client(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Hello from Client with id " + getId() + " config " + getConfig());

    Message m = new Message();
    m.setC_val(123);
    m.setType(MessageTypes.NULL);
    sendToProposers(m);

  }

  private void sendToProposers(Message m){
    String [] hostPort = getConfig().get("proposers").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  @Override
  protected void deliverMessage(Message m) {}
}