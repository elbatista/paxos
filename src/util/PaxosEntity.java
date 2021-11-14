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
//import java.util.ArrayList;
import java.util.HashMap;

import src.message.Message;

public abstract class PaxosEntity {
  private int id;
  private HashMap<String, String> config = new HashMap<>();
  public static int MCAST_DATAGRAM_PACKET_SIZE = 5000;
  public static int ACCEPTORS_QUORUM = 2;
  public static int NUM_OF_PROPOSERS = 2;

  //private ArrayList<ConsensusInstance> consensusInstances = new ArrayList<>();

  public HashMap<String, String> getConfig() {
    return config;
  }

  public void setConfig(HashMap<String, String> config) {
    this.config = config;
  }

  public int getId() {
    return id;
  }
  
  public void setId(int id) {
    this.id = id;
  }

  public PaxosEntity(int id, HashMap<String, String> config){
    setId(id);
    setConfig(config);
  }

  protected void createListener(String host, int port) {
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
            deliverMessage(m);

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

  protected void sendMessage(Message m, String host, int port){
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

  protected abstract void deliverMessage(Message m);

}