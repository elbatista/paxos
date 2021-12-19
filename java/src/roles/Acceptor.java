package src.roles;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Acceptor extends PaxosEntity {

  private BlockingQueue<Message> proposer_1A_messages = new LinkedBlockingQueue<>();
  private BlockingQueue<Message> proposer_2A_messages = new LinkedBlockingQueue<>();

  public Acceptor(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("acceptors");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    create_listener(host, port);
    // create 2 threads for each type of message
    // since threads may contend to process messages for the same instance, I used a lock for each instance (also the instances set is a concurrent HashMap)
    create_local_threads();
    create_local_threads();
  }

  private void create_local_threads() {
    new Thread(new Runnable(){public void run(){while(true) try{message_1A(proposer_1A_messages.take());}catch(Exception e){e.printStackTrace();}}}).start();
    new Thread(new Runnable(){public void run(){while(true) try{message_2A(proposer_2A_messages.take());}catch(Exception e){e.printStackTrace();}}}).start();
  }

  @Override
  protected void deliver_message(Message m) {
    try{
      switch(m.get_type()){
        case PHASE_1A: proposer_1A_messages.put(m); return;
        case PHASE_2A: proposer_2A_messages.put(m); return;
        default: return;
      }
    }
    catch(InterruptedException e){
      e.printStackTrace();
    }
  }

  private void message_1A(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    instance.lock();
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
    instance.unlock();
  }

  private void message_2A(Message m) {
    ConsensusInstance instance = get_instance(m.get_instance_id());
    instance.lock();
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
    }
    instance.unlock();
  }

}