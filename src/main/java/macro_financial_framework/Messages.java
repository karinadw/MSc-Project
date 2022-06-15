package macro_financial_framework;

import simudyne.core.graph.Message;

public class Messages {

    public static class ApplyForInvestor extends Message {}
    public static class FindInvestor extends Message {}

    public static class InvestorOfFirm extends Message {
        public long investorID;
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

    public static class JobApplication extends Message.Integer {}

    public static class FirmInformation extends Message {
        public int sector;
        public int vacancies;
    }

    public static class WorkerPayment extends Message {
        public double wage;
    }

    public static class AnnualCheck extends Message{}

    public static class Fired extends Message{}


}

