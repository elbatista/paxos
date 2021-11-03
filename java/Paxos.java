import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Paxos {

  public Paxos(String role, int id, String config_file){
    switch (role) {
      case "acceptor" : new Acceptor(id, parseConfigFile(config_file)); break;
      case "proposer" : new Proposer(id, parseConfigFile(config_file)); break;
      case "learner" : new Learner(id, parseConfigFile(config_file)); break;
      case "client" : new Client(id, parseConfigFile(config_file)); break;
      default: break;
    }
  }

  private HashMap<String, String> parseConfigFile(String config_file) {
    HashMap<String, String> config = new HashMap<>();
    BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(config_file));
			String line = reader.readLine();
			while (line != null) {
        config.put(line.split(" ")[0], line.split(" ")[1]+":"+line.split(" ")[2]);
				line = reader.readLine();
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