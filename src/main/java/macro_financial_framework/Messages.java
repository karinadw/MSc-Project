package macro_financial_framework;

import simudyne.core.graph.Message;

public class Messages {
    public static class Job_update extends Message.Integer {}

    public static class WorkerHired extends Message.Empty {}

    public static class FirmHiredWorker extends Message.Empty {}

    public static class JobApplication extends Message.Integer {}

    public static class FirmInformation extends Message.Integer {}

    public static class FirmVacancies extends Message.Integer {}

}

