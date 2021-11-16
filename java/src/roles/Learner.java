package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.util.PaxosEntity;

public class Learner extends PaxosEntity {
  
  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("learners");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Learner " + get_id() + "; config: " + conf);
    create_listener(host, port);
  }

  @Override
  protected void deliver_message(Message m) {
    switch(m.get_type()){
      case DECIDE: message_decision(m);
      default: return;
    }
  }

  private void message_decision(Message m) {
    get_instance(m.get_instance_id()).set_decided_value(m.get_v_val());;
    print_instances();
  }

} 