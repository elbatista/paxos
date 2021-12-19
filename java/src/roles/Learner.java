package src.roles;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Learner extends PaxosEntity {

  private int current_instance = 1;
  private int gratest_instance = 0;
  private BlockingQueue<Message> decision_messages = new LinkedBlockingQueue<>();
  private BlockingQueue<Message> fill_gap_messages = new LinkedBlockingQueue<>();
  private HashMap<Integer, ConsensusInstance> instances = new HashMap<>();

  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("learners");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    create_listener(host, port);
    create_local_theads();
  }

  private void create_local_theads() {
    new Thread(new Runnable(){public void run(){while(true) process_instances();}}).start();
    new Thread(new Runnable(){public void run(){while(true) process_decision_messages();}}).start();
    new Thread(new Runnable(){public void run(){while(true) process_fill_gap_messages();}}).start();
    new Thread(new Runnable(){public void run(){while(true) send_fill_gap();}}).start();
    new Thread(new Runnable(){public void run(){while(true) send_catch_up();}}).start();
  }

  @Override
  protected void deliver_message(Message m) {
    try {
      switch(m.get_type()){
        case PHASE_2B: decision_messages.put(m); return;
        case FILL_GAP: fill_gap_messages.put(m); return;
        case CATCH_UP: process_catch_up_messages(m); return;
        default: return;
      }
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void send_catch_up(){
    Message m = new Message();
    m.set_type(MessageTypes.CATCH_UP);
    send_to_proposers(m);
    try {Thread.sleep(2000);} catch (InterruptedException e1) {}
  }

  private void send_fill_gap(){
    try {
      Thread.sleep(10);
      getLock().lock();
      if(current_instance <= gratest_instance){
        Message m = new Message();
        m.set_instance_id(current_instance);
        m.set_type(MessageTypes.FILL_GAP);
        send_to_proposers(m);
      }
      getLock().unlock();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void process_catch_up_messages(Message m) {
    getLock().lock();
    if(m.get_instance_id() > gratest_instance)
      gratest_instance = m.get_instance_id();
    getLock().unlock();
  }

  private void process_decision_messages(){
    try {
      Message m = decision_messages.take();
      int iid = m.get_instance_id();
      ConsensusInstance instance = null;
      getLock().lock();
      instance = instances.get(iid);
      if(instance == null){
        instance = new ConsensusInstance(iid);
        instances.put(iid, instance);
        if(iid > gratest_instance) gratest_instance = iid;
      }
      instance.add_message_2B(m);
      if(instance.has_quorum_2B() && !instance.is_decided()){
        if(instance.has_quorum_2B_in_highest_round())
          instance.set_decided_value(m.get_v_val());
      }
      getLock().unlock();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void process_fill_gap_messages(){
    try {
      Message m = fill_gap_messages.take();
      int iid = m.get_instance_id();
      ConsensusInstance instance = null;
      getLock().lock();
      instance = instances.get(iid);
      if(instance == null){
        instance = new ConsensusInstance(iid);
        instance.set_decided_value(m.get_v_val());
        instances.put(iid, instance);
      }
      getLock().unlock();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void process_instances(){
    getLock().lock();
    ConsensusInstance instance = instances.get(current_instance);
    if(instance != null && instance.is_decided()){
      instance.execute();
      current_instance++;
      getLock().unlock();
    }
    else {
      getLock().unlock();
      Thread.yield();
    }
  }

}