package src.roles;
import java.util.HashMap;
import java.util.Scanner;

import src.message.Message;
import src.message.MessageTypes;
import src.util.PaxosEntity;

public class Client extends PaxosEntity {

  public Client(int id, HashMap<String, String> config){
    super(id, config);
    Scanner scanner = new Scanner(System.in);
    while(scanner.hasNextLine()){
      Message m = new Message();
      m.set_client_value(Integer.valueOf(scanner.nextLine()));
      m.set_type(MessageTypes.CLIENT);
      send_to_proposers(m);
    }
    scanner.close();
  }

  @Override
  protected void deliver_message(Message m) {}
}