package src.roles;
import java.util.ArrayList;
import java.util.HashMap;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Proposer extends PaxosEntity {

  private int instance_count = 0;

  // private void inc_c_rnd(long target){
  //   while(getC_rnd() <= target){
  //     c_rnd++;
  //     while((getC_rnd() % NUM_OF_PROPOSERS) != getId()){
  //       c_rnd++;
  //     }
  //   }
  // }

  public Proposer(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("proposers");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Proposer " + get_id() + "; config: " + conf);
    create_listener(host, port);
  }

  @Override
  protected void deliver_message(Message m) {
    switch(m.get_type()){
      case CLIENT: message_from_client(m); break;
      case PHASE_1B: message_1B(m); break;
      case PHASE_2B: message_2B(m); break;
      default: return;
    }    
  }

  private void message_1B(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    // upon receiving (PHASE 1B, rnd, v-rnd, v-val) from Qa such that c-rnd = rnd
    //    k ← largest v-rnd value received
    //    V ← set of (v-rnd,v-val) received with v-rnd = k
    //    if k = 0 then let c-val be v
    //    else c-val ← the only v-val in V
    //    send (PHASE 2A, c-rnd, c-val) to acceptors
    if(m.get_rnd() == instance.get_c_rnd()){
      instance.add_message_1B(m);
      if(instance.has_quorum_1B() && !instance.sent_2A()){
        System.out.println("Message 1B from quorum for round "+instance.get_c_rnd());

        // k ← largest v-rnd value received
        long k = instance.get_largest_v_rnd();
        System.out.println("K = " + k);

        // if k = 0 then let c-val be v
        if(k == 0){
          m.set_c_val(instance.get_v());
        }
        else {
          // V ← set of (v-rnd,v-val) received with v-rnd = k
          ArrayList<Message> V = instance.get_set_V(k);
          
          // else c-val ← the only v-val in V
          long the_only_v_val = V.get(0).get_v_val();
          for(int i=1; i< V.size(); i++){
            if(V.get(i).get_v_val() != the_only_v_val){
              System.err.println("Error: v-rnd is not unique in V for round " + instance.get_c_rnd());
              System.exit(0);
            }
            the_only_v_val = V.get(i).get_v_val();
          }
          System.out.println("the_only_v_val = " + the_only_v_val);
          m.set_c_val(the_only_v_val);
        }
        
        m.set_type(MessageTypes.PHASE_2A);
        send_to_acceptors(m);
        instance.set_sent_2A(true);
      }
    }
  }

  private void message_2B(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    // upon receiving (PHASE 2B, v-rnd, v-val) from Qa
    //    if for all received messages: v-rnd = c-rnd then
    //      send (DECISION, v-val) to learners
    if(m.get_v_rnd() == instance.get_c_rnd()){
      instance.add_message_2B(m);
      if(instance.has_quorum_2B() && !instance.sent_decide()){
        System.out.println("Message 2B from quorum for round "+instance.get_c_rnd()+". Sending decision value: " + m.get_v_val());
        // v-val is already set in message.v_val
        m.set_type(MessageTypes.DECIDE);
        send_to_learners(m);
        instance.set_sent_decide(true);
      }
    }
  }

  private void message_from_client(Message m) {
    System.out.println("Message from client with value: "+m.get_client_value());

    // each message from client generates a new consensus instance
    ConsensusInstance instance = new ConsensusInstance(instance_count);
    instance_count++;

    // To propose value v:
    //    increase c-rnd to an arbitrary unique value
    //    send (PHASE 1A, c-rnd) to acceptors
    
    // store in v the value that I want to propose (the value from client)
    instance.set_v(m.get_client_value());
    instance.increment_c_rnd(get_id());
    
    m.set_instance_id(instance.get_id());
    m.set_type(MessageTypes.PHASE_1A);
    m.set_c_rnd(instance.get_c_rnd());
    
    send_to_acceptors(m);
  }
  
  private void send_to_learners(Message m) {
    String [] hostPort = get_config().get("learners").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  private void send_to_acceptors(Message m) {
    String [] hostPort = get_config().get("acceptors").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

}