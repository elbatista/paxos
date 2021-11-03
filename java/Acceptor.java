import java.util.HashMap;

public class Acceptor extends PaxosEntity {
  public Acceptor(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Hello from Acceptor with id " + getId() + " config " + getConfig());
  }
} 