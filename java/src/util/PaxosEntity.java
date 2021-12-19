package src.util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import src.message.Message;

public abstract class PaxosEntity{
  
  private int id;
  private HashMap<String, String> config = new HashMap<>();
  public static int MCAST_DATAGRAM_PACKET_SIZE = 5000;
  public static int ACCEPTORS_QUORUM = 2;
  public static int NUM_OF_PROPOSERS = 2;
  private ConcurrentHashMap<Integer, ConsensusInstance> consensus_instances = new ConcurrentHashMap<>();
  private ReentrantLock lock = new ReentrantLock();

  public PaxosEntity(int id, HashMap<String, String> config){
    this(id, config, false);
  }

  public PaxosEntity(int id, HashMap<String, String> config, boolean print_instances){
    this.id = id;
    this.config = config;
    if(print_instances && get_id() ==0 )
      new Thread(new Runnable() {
        @Override
        public void run() {
          while(true){
            try {Thread.sleep(5000);} catch (InterruptedException e) {}
            System.out.println("Known instances so far: " + consensus_instances.size());
          }
        }
      }).start();
  }

  protected ConsensusInstance get_instance(int instance_id){
    ConsensusInstance i = null;
    try{i = consensus_instances.get(instance_id);}catch(IndexOutOfBoundsException e){}
    if(i == null){
      i = new ConsensusInstance(instance_id);
      add_instance(i);
    }
    return i;
  }

  protected void create_listener(String host, int port) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        MulticastSocket socket = null;
        InetAddress group = null;
        try {
          socket = new MulticastSocket(port);
          group = InetAddress.getByName(host);
          socket.joinGroup(group);

          while(true){
            byte[] recvBuf = new byte[MCAST_DATAGRAM_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            socket.receive(packet);
            
            ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream(byteStream));
            Message m = (Message) is.readObject();
            is.close();

            // Call up to the receiver
            deliver_message(m);

            if(m == null){
              System.err.println("Error: null message received !!!!");
              return;
            }
          }
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
        finally{
          if(socket != null) {
            try {
              socket.leaveGroup(group);
            } catch (IOException e) {
              e.printStackTrace();
            }
            socket.close();
          }
        }
      }
    }).start();
  }

  protected void send_message(Message m, String host, int port){

    // When testing with message loss (5% of message loss):
    if(new Random().nextInt(100) <= 5) return;

    MulticastSocket socket = null;
    try {
      socket = new MulticastSocket(port);
      InetAddress address = InetAddress.getByName(host);
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream(MCAST_DATAGRAM_PACKET_SIZE);
      ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
      os.flush();
      os.writeObject(m);
      os.flush();
      byte[] sendBuf = byteStream.toByteArray();
      DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
      socket.send(packet);
      socket.close();
      os.close();
    }
    catch (IOException e){}
  }

  protected void send_to_proposers(Message m){
    String [] hostPort = get_config().get("proposers").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  protected void send_to_learners(Message m) {
    String [] hostPort = get_config().get("learners").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  protected void send_to_acceptors(Message m) {
    String [] hostPort = get_config().get("acceptors").split(":");
    send_message(m, hostPort[0], Integer.valueOf(hostPort[1]));
  }

  protected abstract void deliver_message(Message m);
  protected ReentrantLock getLock(){return lock;}
  protected ConsensusInstance get_existing_instance(int instance_id){return consensus_instances.get(instance_id);}
  protected void add_instance(ConsensusInstance i){consensus_instances.put(i.get_id(), i);}
  public HashMap<String, String> get_config() {return config;}
  public int get_id() {return id;}
  
}