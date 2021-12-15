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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import src.message.Message;

public abstract class PaxosEntity {
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
    set_id(id);
    set_config(config);
    if(print_instances)
      new Thread(new Runnable() {
        @Override
        public void run() {
          while(true){
            try {
              Thread.sleep(2000);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
            System.out.println("Known instances so far: " + getNumInstances());
          }
        }
      }).start();
  }

  protected ReentrantLock getLock(){
    return lock;
  }

  protected int getNumInstances(){
    return consensus_instances.size();
  }

  protected ConsensusInstance get_instance(int instance_id){
    ConsensusInstance i = null;
    
    try{
      i = consensus_instances.get(instance_id);
    }
    catch(IndexOutOfBoundsException e){}

    if(i == null){
      i = new ConsensusInstance(instance_id);
      add_instance(i);
    }
    return i;
  }

  protected ConsensusInstance get_existing_instance(int instance_id){
    return consensus_instances.get(instance_id);
  }

  protected void add_instance(ConsensusInstance i){
    consensus_instances.put(i.get_id(), i);
  }

  public HashMap<String, String> get_config() {
    return config;
  }

  public void set_config(HashMap<String, String> config) {
    this.config = config;
  }

  public int get_id() {
    return id;
  }
  
  public void set_id(int id) {
    this.id = id;
  }

  protected void print_instances() {
    consensus_instances.forEach((index, instance) -> {
      System.out.println(instance.get_id()+": "+instance.get_decided_value());
    });
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
      //int byteCount = packet.getLength();
      socket.send(packet);
      socket.close();
      os.close();
    }
    catch (IOException e){}
  }

  protected abstract void deliver_message(Message m);

}