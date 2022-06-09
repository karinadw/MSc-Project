
import macro_financial_framework.MacroFinancialModel;
import simudyne.nexus.Server;

public class Main {
  public static void main(String[] args) {

    Server.register("Macro-Financial Model", MacroFinancialModel.class);

    Server.run(args);
  }
}
