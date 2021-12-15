package src.roles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Proposer extends PaxosEntity {

  private int instance_count = 0;
  private BlockingQueue<ConsensusInstance> pending_instances = new LinkedBlockingQueue<>();
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
    System.out.println("Running Proposer " + get_id() + "; config: " + conf);
    create_listener(host, port);
    create_clients_listener();
    create_acceptors_1B_listener();
    create_acceptors_2B_listener();
    create_pending_instances_verifier();
  }

  private void create_clients_listener() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(true){
          try {
            message_from_client(client_messages.take());
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  private void create_acceptors_1B_listener() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(true){
          try {
            message_1B(acceptor_1B_messages.take()); 
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  private void create_acceptors_2B_listener() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(true){
          try {
            message_2B(acceptor_2B_messages.take()); 
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  private void create_pending_instances_verifier() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(true)
          verify_pending_instances();
      }
    }).start();
  }

  @Override
  protected void deliver_message(Message m) {
    try{
      switch(m.get_type()){
        case CLIENT: client_messages.put(m); return;
        case PHASE_1B: acceptor_1B_messages.put(m); return;
        case PHASE_2B: acceptor_2B_messages.put(m); return;
        case FILL_GAP: learner_messages.put(m); return;
        default: return;
      }
    }
    catch(InterruptedException e){
      e.printStackTrace();
    }
  }

  private void message_fill_gap(Message m) {
    // getLock().lock();
    // try {
      ConsensusInstance instance = get_existing_instance(m.get_instance_id());
      if(instance == null) {
        System.out.println("Received fill gap, for unknown instance: "+m.get_instance_id());
        return;
      }

      if(instance.is_decided()){
        //System.out.println("Received fill gap, for instance: "+instance.get_id());
        m.set_v_val(instance.get_decided_value());
        m.set_type(MessageTypes.DECIDE);
        send_to_learners(m);
        //System.out.println("Just sent decision for instance " + instance.get_id());
      }
      else {
        getLock().lock();
        if(instance.get_v() > -1 && !pending_instances.contains(instance)){
          System.out.println("Received fill gap, for instance not yet decided: " + instance.get_id() + "; Adding to pending");
          pending_instances.add(instance);
        }
        getLock().unlock();
      }
    // }
    // finally{
    //   getLock().unlock();
    // }
  }

  private void message_1B(Message m) {
    // getLock().lock();
    // try {
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
          //System.out.println("Message 1B from quorum for round "+instance.get_c_rnd());

          // k ← largest v-rnd value received
          long k = instance.get_largest_v_rnd();
          //System.out.println("K = " + k);

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
            //System.out.println("the_only_v_val = " + the_only_v_val);
            m.set_c_val(the_only_v_val);
          }
          
          m.set_type(MessageTypes.PHASE_2A);
          send_to_acceptors(m);
          instance.set_sent_2A(true);
        }
      }
    // }
    // finally{
    //   getLock().unlock();
    // }
  }

  private void message_2B(Message m) {
    // getLock().lock();
    // try{
      ConsensusInstance instance = get_instance(m.get_instance_id());
      // upon receiving (PHASE 2B, v-rnd, v-val) from Qa
      //    if for all received messages: v-rnd = c-rnd then
      //      send (DECISION, v-val) to learners
      if(m.get_v_rnd() == instance.get_c_rnd()){
        instance.add_message_2B(m);
        if(instance.has_quorum_2B() && !instance.is_decided()){
          //System.out.println("Message 2B from quorum for round "+instance.get_c_rnd()+". Sending decision value: " + m.get_v_val());
          // v-val is already set in message.v_val
          m.set_type(MessageTypes.DECIDE);
          instance.set_decided_value(m.get_v_val());
          getLock().lock();
          pending_instances.remove(instance);
          getLock().unlock();
          send_to_learners(m);
        }
      }
    // }
    // finally{
    //   getLock().unlock();
    // }
  }

  private void message_from_client(Message m) {
    // getLock().lock();
    // try{
      // each message from client generates a new consensus instance
      ConsensusInstance instance = new ConsensusInstance(instance_count);
      instance_count++;

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
      getLock().lock();
      try {
        pending_instances.put(instance);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      getLock().unlock();
      
      send_to_acceptors(m);
    // }
    // finally{
    //   getLock().unlock();
    // }
  }

  private void verify_pending_instances(){
    try {Thread.sleep(100);} catch (InterruptedException e) {}

    System.out.println("Pending instances: " + pending_instances.size());
    System.out.println("2B Messages: " + acceptor_2B_messages.size());

    getLock().lock();
    try {
      ConsensusInstance instance = pending_instances.peek();
      if(instance != null){
        if(!instance.is_decided() && instance.timeout()){
          instance.increment_c_rnd(get_id());
          Message m = new Message();
          m.set_instance_id(instance.get_id());
          m.set_type(MessageTypes.PHASE_1A);
          m.set_c_rnd(instance.get_c_rnd());
          //System.out.println("Resending P1A instance " + instance.get_id() + "; new c-rnd " + instance.get_c_rnd());
          send_to_acceptors(m);
        }
      }
    
      // for (ConsensusInstance instance : pending_instances) {
      //   if(!instance.is_decided() && instance.timeout()){
      //     //System.out.println("Instance timeout " + instance.get_id() + "; c-rnd " + instance.get_c_rnd());
          
      //     return;
      //   }
      // }
    }
    finally{
      getLock().unlock();
    }
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