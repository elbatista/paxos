package src.roles;
import java.util.HashMap;
import java.util.Scanner;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Client extends PaxosEntity {

  public Client(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Running Client " + get_id() + "; config: " + get_config().get("clients"));
    Scanner scanner = new Scanner(System.in);
    while(scanner.hasNextLine()){
      Message m = new Message();
      m.set_client_value(Integer.valueOf(scanner.nextLine()));
      m.set_type(MessageTypes.CLIENT);
      send_to_proposers(m);
      // try {
      //   Thread.sleep(20);
      // }
      // catch (InterruptedException e) {
      //   e.printStackTrace();
      // }
    }
    scanner.close();
  }

  private void send_to_proposers(Message m){
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  @Override
  protected void deliver_message(Message m) {}
}