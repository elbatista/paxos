package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Client extends PaxosEntity {

  public Client(int id, HashMap<String, String> config, int value_to_propose){
    super(id, config);
    System.out.println("Running Client " + get_id() + "; config: " + get_config().get("clients"));

    Message m = new Message();
    if(value_to_propose == -1)
      value_to_propose = 123;

    m.set_client_value(value_to_propose);
    m.set_type(MessageTypes.CLIENT);
    send_to_proposers(m);
  }

  private void send_to_proposers(Message m){
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  @Override
  protected void deliver_message(Message m) {}
}