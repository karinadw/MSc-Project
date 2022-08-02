package SimpleEconomyModel.utils;

import simudyne.core.graph.Message;

public class Messages {

    public static class ApplyForInvestor extends Message {}
    public static class FindInvestor extends Message {}

    public static class InvestorOfFirm extends Message {
        public long investorID;
    }

    public static class FirmProperties extends Message {
        public double wage;
        public int good;
        public int goodToPurchase;
    }

    public static class FirmGood extends Message {
        public int good;
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

    public static class JobCheck extends Message{
        public double productivity;
    }

    public static class FirmSupply extends Message {
        public double price;
        public long output;
//        public int sector;
    }

    public static class HouseholdDemand extends Message {
//        public int sectorOfGoods;
        public double consumptionBudget;
    }

    public static class HouseholdOrFirmWantsToPurchase extends Message {
        public long bought ;
        public int demand;
    }

    public static class PurchaseCompleted extends Message {
        public double spent;
    }

    public static class Fired extends Message{}

    public static class FirmsPrice extends Message{
        public double output;
        public double price;
    }

    public static class FirmsPriceDemandedGoods extends Message{
        public double output;
        public double price;
    }

    public static class PayInvestors extends Message {
        public double dividend;
    }

    public static class AveragePrice extends Message {
        public double averagePrice;
    }
    public static class Unemployed extends Message {
        public int sector;
    }
    public static class FirmGetAvailableWorkers extends Message {
        public int sector;
    }

    public static class AvailableWorkersInYourSector extends Message {
        public int workers;
    }
    public static class CurrentUnemployment extends Message {
        public double unemployment;
    }

    public static class HealthyFirmAccountMessage extends Message {
        public HealthyFirmAccount healthyFirmAccount;
    }

    public static class BailoutRequestMessage extends Message {
        public double debt;
    }

    public static class BailoutPackageMessage extends Message {
        public double price;
        public double wage;
    }

    public static class DepositsMessage extends Message {
        public double cash;
    }

    public static class BankruptcyMessage extends Message.Empty {}

    public static class PurchaseIntermediateGood extends Message {
        public int demand;
    }

    public static class StockOfIntermediateGood extends Message {
        public long stock;
        public double price;
    }

    public static class IntermediateGoodBought extends Message {
        public int quantity;
        public double spent;

        public int good;
    }

    public static class DemandOfIntermediateGood extends Message {
        public int demand;
        public int bought;
    }

    public static class PaidDebtOfIndebtedFirm extends Message {
        public double debt;
    }

    public static class Employed extends Message.Empty{}

    public static class InvestorPaysRevival extends Message {
        public double debt;
    }

    public static class SoldGoodsAndPrices extends Message {
        public int quantitySold;
        public double price;
    }

}

