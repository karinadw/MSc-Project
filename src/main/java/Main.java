import SimpleEconomyModel.SimpleEconomyModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {

    Server.register("Macro-Financial Model", SimpleEconomyModel.class);

    Server.run(args);
  }
}
