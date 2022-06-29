package macro_financial_framework;

import simudyne.core.abm.Agent;
import simudyne.core.abm.Action;

public class GoodsMarket extends Agent<MacroFinancialModel.Globals> {

    public int goodTraded;
    public boolean competitive;

    public static Action<GoodsMarket> setPriceOfGood() {
        return Action.create(GoodsMarket.class, good -> {

        });
    }
}
