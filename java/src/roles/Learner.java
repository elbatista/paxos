package src.roles;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import src.message.Message;
import src.message.MessageTypes;
import src.util.ConsensusInstance;
import src.util.PaxosEntity;

public class Learner extends PaxosEntity {

  private Semaphore sema = new Semaphore(0);
  private int current_instance = 1, retries = 0;
  
  public Learner(int id, HashMap<String, String> config){
    super(id, config);
    String conf = get_config().get("learners");
    String [] configSplit = conf.split(":");
    String host = configSplit[0];
    int port = Integer.valueOf(configSplit[1]);
    System.out.println("Running Learner " + get_id() + "; config: " + conf);
    create_listener(host, port);
    create_executor();
  }

  private void create_executor() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        while(true)
          process_instances();
      }
    }).start();
  }

  @Override
  protected void deliver_message(Message m) {
    switch(m.get_type()){
      case DECIDE: message_decision(m); break;
      default: return;
    }
  }

  private void process_instances(){
    try {
      // executor thread
      // wakes up with a new instance (decision message) or timeout
      // keeps track of the last executed instance
      // starts in instance zero
      // executes in order of instance id
      // when next instance is not arriving (say 10 retries), or timeout, sends message to proposer asking for the instance

      //boolean acquired = sema.tryAcquire(50, TimeUnit.MILLISECONDS);
      
      sema.acquire();
      
      getLock().lock();

      // // if time out or 10 retries send message to proposer
      // if(!acquired){// || retries >= 10){
      //   Message m2 = new Message();
      //   m2.set_instance_id(current_instance);
      //   m2.set_type(MessageTypes.FILL_GAP);
      //   send_to_proposers(m2);
      //   retries = 0;
      //   //System.out.println("Asking for instance " +current_instance);
      //   return;
      // }

      ConsensusInstance instance = get_existing_instance(current_instance);
      if(instance != null){
        instance.execute();
        //retries = 0;
        current_instance++;
      }
      // else  {
      //   retries++;
      // }

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    finally{
      getLock().unlock();
    }
  }

  private void message_decision(Message m) {
    getLock().lock();
    try {
      // get_instance also inserts the instance in the set (it creates a new one if not exists)
      ConsensusInstance instance = get_instance(m.get_instance_id());
      instance.set_decided_value(m.get_v_val());
      // signals semaphore
      sema.release();
    }
    finally{
      getLock().unlock();
    }
  }

  private void send_to_proposers(Message m){
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }
}