import java.util.HashMap;

public class Proposer extends PaxosEntity {
  public Proposer(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Hello from Proposer with id " + getId() + " config " + getConfig());
  }
} 