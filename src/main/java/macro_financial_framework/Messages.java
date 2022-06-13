package macro_financial_framework;

import simudyne.core.graph.Message;

public class Messages {

    public static class Hired extends Message {
        public long firmID;
    }

    public static class NewEmployee extends Message {
        public long workerID;
    }

    public static class FirmHiredWorker extends Message.Empty {}

    public static class JobApplication extends Message.Integer {}

    public static class FirmInformation extends Message {
        public int sector;
        public int vacancies;
    }

    public static class FirmVacancies extends Message.Integer {}

}

