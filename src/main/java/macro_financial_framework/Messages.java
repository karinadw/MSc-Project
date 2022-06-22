package macro_financial_framework;

import simudyne.core.graph.Message;

public class Messages {

    public static class ApplyForInvestor extends Message {}
    public static class FindInvestor extends Message {}

    public static class InvestorOfFirm extends Message {
        public long investorID;
    }

    public static class FirmWage extends Message {
        public double wage;
    }

    public static class priceOfGoods extends Message {
        public double price;
    }

    public static class FirmAssignedToInvestor extends Message {
        public long firmID;
    }
    public static class Hired extends Message {
        public long firmID;
    }

    public static class NewEmployee extends Message {
        public long workerID;

    }

    public static class Productivity extends Message {
        public double productivity;
    }

    public static class JobApplication extends Message {
        public int sector;
        public double productivity;
    }

    public static class FirmInformation extends Message {
        public int sector;
        public int vacancies;
    }

    public static class WorkerPayment extends Message {
        public double wage;
    }

    public static class AnnualCheck extends Message{
        public double productivity;
    }

    public static class FirmSupply extends Message {
        public double price;
        public long output;
        public int sector;
    }

    public static class HouseholdDemand extends Message {
        public int sectorOfGoods;
        public double consumptionBudget;
    }

    public static class HouseholdWantsToPurchase extends Message {
        public long HouseholdID;
        public int demand;
    }

    public static class PurchaseCompleted extends Message {
        public double spent;
    }

    public static class Fired extends Message{}

    public static class FirmsPrice extends Message{
        public double output;
        public double price;
        public double productivity;
    }

    public static class Wages extends Message {
        public double wage;
    }

    public static class PayInvestors extends Message {
        public double dividend;
    }


}

