package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Workers extends Agent<MacroFinancialModel.Globals> {

    public int sector_skills;
    @Variable
    public double wealth;

    public enum Status {EMPLOYED, UNEMPLOYED, UNEMPLOYED_APPLIED}

    public Status status = Status.UNEMPLOYED; //everyone starts by being unemployed


    public static Action<Workers> applyForJob() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.status == Status.UNEMPLOYED) {
                        worker.getLinks(Links.WorkerToLabourMarketLink.class).send(Messages.JobApplication.class, worker.sector_skills);
                        worker.status = Status.UNEMPLOYED_APPLIED;
                    }
                });
    }

    public static Action<Workers> updateAvailability() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.hasMessageOfType(Messages.Hired.class)) {
                        long firmID = worker.getMessageOfType(Messages.Hired.class).firmID;
                        worker.addLink(firmID, Links.WorkerToFirmLink.class);
                        worker.status = Status.EMPLOYED;
                    }

                });
    }

    public static Action<Workers> receiveSalary() {
        return Action.create(Workers.class, worker -> {
            if (worker.hasMessageOfType(Messages.WorkerPayment.class)){
                worker.wealth += worker.getMessageOfType(Messages.WorkerPayment.class).wage;
            }
        });
    }

//    public static Action<Workers> SendFiredInfoToFirm() {
//        return Action.create(Workers.class, worker -> {
//            int numberOfFiredWorkers = worker.getMessageOfType(Messages.NumberOfFiredWorkers.class).getBody();
//            for (int i = 0; i < numberOfFiredWorkers; i++) {
//                if (worker.status == Status.HIRED) {
//                   worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.FiredWorker.class);
//                   worker.removeLinks(Links.WorkerToFirmLink.class);
//                   worker.status = Status.UNEMPLOYED;
//                }
//            }
//        });
//    }
}


