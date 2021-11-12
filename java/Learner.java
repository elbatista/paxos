import java.util.HashMap;

public class Learner extends PaxosEntity {
  
  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    String host = getConfig().get("learners").split(":")[0];
    int port = Integer.valueOf(getConfig().get("learners").split(":")[1]);
    System.out.println("Hello from Learner with id " + getId() + " config host " + host + " port " + port);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    
  }

} 