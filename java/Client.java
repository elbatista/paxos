import java.util.HashMap;

public class Client extends PaxosEntity {
  public Client(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Hello from Client with id " + getId() + " config " + getConfig());
  }
} 