package src.roles;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Acceptor extends PaxosEntity {

  public Acceptor(int id, HashMap<String, String> config){
    super(id, config, true);
    String conf = get_config().get("acceptors");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Acceptor " + get_id() + "; config: " + conf);
    create_listener(host, port);
  }

  @Override
  protected void deliver_message(Message m) {
    switch(m.get_type()){
      case PHASE_1A: message_1A(m); return;
      case PHASE_2A: message_2A(m); return;
      default: return;
    }    
  }

  private void message_1A(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    // upon receiving (PHASE 1A, c-rnd) from proposer
    //    if c-rnd > rnd then
    //      rnd ← c-rnd
    //      send (PHASE 1B, rnd, v-rnd, v-val) to proposer
    if(m.get_c_rnd() > instance.get_rnd()){
      instance.set_rnd(m.get_c_rnd());
      m.set_type(MessageTypes.PHASE_1B);
      m.set_rnd(instance.get_rnd());
      m.set_v_rnd(instance.get_v_rnd());
      m.set_v_val(instance.get_v_val());
      send_to_proposers(m);
    }
    else {
      System.out.println("Ignoring 1A " + m.get_instance_id());
    }
  }

  private void message_2A(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    // upon receiving (PHASE 2A, c-rnd, c-val) from proposer
    //    if c-rnd ≥ rnd then
    //        v-rnd ← c-rnd
    //        v-val ← c-val
    //        send (PHASE 2B, v-rnd, v-val) to proposer
    if(m.get_c_rnd() >= instance.get_rnd()){
      instance.set_v_rnd(m.get_c_rnd());
      instance.set_v_val(m.get_c_val());
      m.set_type(MessageTypes.PHASE_2B);
      m.set_v_rnd(instance.get_v_rnd());
      m.set_v_val(instance.get_v_val());
      send_to_proposers(m);
      //System.out.println("Sending 2A " + m.get_instance_id() +" "+ m.get_c_rnd() +" "+ instance.get_rnd());
    }
    else {
      System.out.println("Ignoring 2B " + m.get_instance_id());
    }
  }

  private void send_to_proposers(Message m) {
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}