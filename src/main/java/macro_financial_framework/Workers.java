package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

public class Workers extends Agent<MacroFinancialModel.Globals> {

    public int sector_skills;

    public enum Status{HIRED, UNEMPLOYED, UNEMPLOYED_APPLIED}

    public Status status = Status.UNEMPLOYED; //everyone starts by being unemployed



    public static Action<Workers> applyForJob() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.status == Status.UNEMPLOYED){
                        worker. getLinks(Links.WorkerToLabourMarketLink.class).send(Messages.JobApplication.class, worker.sector_skills);
                        worker.status = Status.UNEMPLOYED_APPLIED;
                    }
                });
    }

    public static Action<Workers> updateAvailability() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.hasMessageOfType(Messages.Hired.class)){
                        long firmID = worker.getMessageOfType(Messages.Hired.class).firmID;
                        worker.addLink(firmID, Links.WorkerToFirmLink.class);
                        worker.status = Status.HIRED;
                    }

                });
    }
}


