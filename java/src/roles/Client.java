package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Client extends PaxosEntity {

  public Client(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Running Client " + getId() + "; config: " + getConfig().get("clients"));

    Message m = new Message();
    m.setClientValue(7567);
    m.setType(MessageTypes.CLIENT);
    sendToProposers(m);
  }

  private void sendToProposers(Message m){
    String [] hostPort = getConfig().get("proposers").split(":");
    sendMessage(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  @Override
  protected void deliverMessage(Message m) {}
}