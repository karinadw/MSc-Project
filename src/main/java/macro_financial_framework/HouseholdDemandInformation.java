package macro_financial_framework;

public class HouseholdDemandInformation {
    public long ID;
//    public int sectorOfGoodsToPurchase;
    public double consumptionBudget;

    public HouseholdDemandInformation(long ID, double consumptionBudget) {
        this.ID = ID;
//        this.sectorOfGoodsToPurchase = sectorOfGoodsToPurchase;
        this.consumptionBudget = consumptionBudget;
    }
}
