import java.util.HashMap;

public class Learner extends PaxosEntity {
  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    System.out.println("Hello from Learner with id " + getId() + " config " + getConfig());
  }
} 