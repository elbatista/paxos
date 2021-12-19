package src;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import src.roles.Acceptor;
import src.roles.Client;
import src.roles.Learner;
import src.roles.Proposer;

public class Paxos {

  public Paxos(String role, int id, String config_file){
    switch (role) {
      case "acceptor" : new Acceptor(id, parse_config_file(config_file)); break;
      case "proposer" : new Proposer(id, parse_config_file(config_file)); break;
      case "learner" : new Learner(id, parse_config_file(config_file)); break;
      case "client" : new Client(id, parse_config_file(config_file)); break;
      default: break;
    }
  }

  private HashMap<String, String> parse_config_file(String config_file) {
    HashMap<String, String> config = new HashMap<>();
    BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(config_file));
			String line;
			for (line = reader.readLine(); line != null; line = reader.readLine()) {
        String [] lineSplit = line.split("\\s+");
        config.put(lineSplit[0], lineSplit[1]+":"+lineSplit[2]);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    return config;
  }

  public static void main(String[] args){
    new Paxos(args[0], Integer.valueOf(args[1]), args[2]);  
  }

}