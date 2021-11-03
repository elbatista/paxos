import java.util.HashMap;

public class PaxosEntity {
  private int id;
  private HashMap<String, String> config = new HashMap<>();

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

}  