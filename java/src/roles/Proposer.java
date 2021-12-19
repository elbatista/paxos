package src.roles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Proposer extends PaxosEntity {

  private AtomicInteger i_count = new AtomicInteger(0);
  private BlockingQueue<Message> client_messages = new LinkedBlockingQueue<>();
  private BlockingQueue<Message> acceptor_1B_messages = new LinkedBlockingQueue<>();
  private BlockingQueue<Message> acceptor_2B_messages = new LinkedBlockingQueue<>();
  private BlockingQueue<Message> learner_messages = new LinkedBlockingQueue<>();

  public Proposer(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("proposers");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    create_listener(host, port);
    // create 3 threads for each type of message
    // since threads may contend to process messages for the same instance, I used a lock for each instance (also the instances set is a concurrent HashMap)
    create_local_threads();
    create_local_threads();
    create_local_threads();
  }

  private void create_local_threads() {
    new Thread(new Runnable(){public void run(){while(true) try{message_from_client(client_messages.take());}catch(Exception e){}}}).start();
    new Thread(new Runnable(){public void run(){while(true) try{message_1B(acceptor_1B_messages.take());}catch(Exception e){}}}).start();
    new Thread(new Runnable(){public void run(){while(true) try{message_2B(acceptor_2B_messages.take());}catch(Exception e){}}}).start();
    new Thread(new Runnable(){public void run(){while(true) try{message_fill_gap(learner_messages.take());}catch(Exception e){}}}).start();
  }

  @Override
  protected void deliver_message(Message m) {
    try{
      switch(m.get_type()){
        case CLIENT: client_messages.put(m); return;
        case PHASE_1B: acceptor_1B_messages.put(m); return;
        case PHASE_2B: acceptor_2B_messages.put(m); return;
        case FILL_GAP: learner_messages.put(m); return;
        case CATCH_UP: learners_catchup(m); return;
        default: return;
      }
    }
    catch(InterruptedException e){
      e.printStackTrace();
    }
  }

  private void learners_catchup(Message m){
    if(i_count.get() >= 0){
      m.set_instance_id(i_count.get());
      send_to_learners(m);
    }
  }

  private void message_fill_gap(Message m) {
    ConsensusInstance instance = get_existing_instance(m.get_instance_id());
    if(instance == null) {
      instance = get_instance(m.get_instance_id());
    }
    instance.lock();
    if(instance.is_decided()){
      m.set_v_val(instance.get_decided_value());
      send_to_learners(m);
    }
    else {
      instance.increment_c_rnd(get_id());
      Message m2 = new Message();
      m2.set_instance_id(instance.get_id());
      m2.set_type(MessageTypes.PHASE_1A);
      m2.set_c_rnd(instance.get_c_rnd());
      send_to_acceptors(m2);
    }
    instance.unlock();
  }

  private void message_1B(Message m) {
    ConsensusInstance instance = get_existing_instance(m.get_instance_id());
    if(instance == null) return;
    instance.lock();
    // upon receiving (PHASE 1B, rnd, v-rnd, v-val) from Qa such that c-rnd = rnd
    //    k ← largest v-rnd value received
    //    V ← set of (v-rnd,v-val) received with v-rnd = k
    //    if k = 0 then let c-val be v
    //    else c-val ← the only v-val in V
    //    send (PHASE 2A, c-rnd, c-val) to acceptors
    if(m.get_rnd() == instance.get_c_rnd()){
      instance.add_message_1B(m);
      if(instance.has_quorum_1B() && !instance.sent_2A()){
        // k ← largest v-rnd value received
        long k = instance.get_largest_v_rnd();
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
              System.err.println("Fatal Error: v-rnd is not unique in V for round " + instance.get_c_rnd());
              System.exit(0);
            }
            the_only_v_val = V.get(i).get_v_val();
          }
          m.set_c_val(the_only_v_val);
        }
        m.set_type(MessageTypes.PHASE_2A);
        instance.set_sent_2A(true);
        send_to_acceptors(m);
      }
    }
    instance.unlock();
  }

  private void message_2B(Message m) {
    ConsensusInstance instance = get_existing_instance(m.get_instance_id());
    if(instance == null) return;
    instance.lock();
    // upon receiving (PHASE 2B, v-rnd, v-val) from Qa
    //    if for all received messages: v-rnd = c-rnd then
    //      send (DECISION, v-val) to learners
    if(m.get_v_rnd() == instance.get_c_rnd()){
      instance.add_message_2B(m);
      if(instance.has_quorum_2B() && !instance.is_decided()){
        // v-val is already set in message.v_val
        m.set_type(MessageTypes.DECIDE);
        instance.set_decided_value(m.get_v_val());
        send_to_learners(m);
      }
    }
    instance.unlock();
  }

  private void message_from_client(Message m) {
    int iid = i_count.incrementAndGet();
    // each message from client generates a new consensus instance
    ConsensusInstance instance = new ConsensusInstance(iid);
    // To propose value v:
    //    increase c-rnd to an arbitrary unique value
    //    send (PHASE 1A, c-rnd) to acceptors
    instance.increment_c_rnd(get_id());
    // store in v the value that I want to propose (the value from client)
    instance.set_v(m.get_client_value());
    m.set_instance_id(instance.get_id());
    m.set_type(MessageTypes.PHASE_1A);
    m.set_c_rnd(instance.get_c_rnd());
    add_instance(instance);
    send_to_acceptors(m);
  }
}