package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.util.PaxosEntity;

public class Learner extends PaxosEntity {
  
  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    String conf = getConfig().get("learners");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Learner " + getId() + "; config: " + conf);
    createListener(host, port);
  }

  @Override
  protected void deliverMessage(Message m) {
    switch(m.getType()){
      case DECIDE: System.out.println("Decision received: " + m.getV_val());
      default: return;
    }
  }

} 