package src.roles;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Learner extends PaxosEntity {

  private int current_instance = 0;
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
    System.out.println("Running Learner " + get_id() + "; config: " + conf);
    create_listener(host, port);
    create_local_theads();
  }

  private void create_local_theads() {
    new Thread(new Runnable(){public void run(){while(true) process_instances();}}).start();
    new Thread(new Runnable(){public void run(){while(true) process_decision_messages();}}).start();
    new Thread(new Runnable(){public void run(){while(true) process_fill_gap_messages();}}).start();
    new Thread(new Runnable(){public void run(){
      try {Thread.sleep(2000);} catch (InterruptedException e1) {}
      while(true) fill_gaps();
    }}).start();
  }

  @Override
  protected void deliver_message(Message m) {
    try {
      switch(m.get_type()){
        case DECIDE: decision_messages.put(m); return;
        case FILL_GAP: fill_gap_messages.put(m); return;
        default: return;
      }
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void process_decision_messages(){
    try {
      Message m = decision_messages.take();
      int iid = m.get_instance_id();
      ConsensusInstance instance = null;

      //if(iid > 2000) {System.out.println("Received weird instance "+iid); System.exit(0);}

      getLock().lock();

      instance = instances.get(iid);

      if(instance == null){
        instance = new ConsensusInstance(iid);
        instance.set_decided_value(m.get_v_val());
        instances.put(iid, instance);
        if(iid > gratest_instance) gratest_instance = iid;
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

      //if(iid > 100000) {System.out.println("Received weird fill gap instance "+iid); System.exit(0);}

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
    if(instance != null){
      instance.execute();
      current_instance++;
      getLock().unlock();
    }
    else {
      getLock().unlock();
      Thread.yield();
    }
  }

  private void fill_gaps(){
    try {

      Thread.sleep(100);

      getLock().lock();

      if(gratest_instance == 0 || current_instance < gratest_instance){
        Message m2 = new Message();
        m2.set_instance_id(current_instance);
        m2.set_type(MessageTypes.FILL_GAP);
        send_to_proposers(m2);
      }

      getLock().unlock();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void send_to_proposers(Message m){
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }
}