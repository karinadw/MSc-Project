package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Households extends Agent<MacroFinancialModel.Globals> {

    public int sector_skills;
    @Variable
    public double wealth;

    public int productivity;

    public enum Status {EMPLOYED, UNEMPLOYED, UNEMPLOYED_APPLIED}

    public Status status = Status.UNEMPLOYED; //everyone starts by being unemployed


    public static Action<Households> applyForJob() {
        return Action.create(Households.class,
                worker -> {
                    if (worker.status == Status.UNEMPLOYED) {
                        worker.getLinks(Links.WorkerToLabourMarketLink.class).send(Messages.JobApplication.class, worker.sector_skills);
                        worker.status = Status.UNEMPLOYED_APPLIED;
                    }
                });
    }

    public static Action<Households> updateAvailability() {
        return Action.create(Households.class,
                worker -> {
                    if (worker.hasMessageOfType(Messages.Hired.class)) {
                        long firmID = worker.getMessageOfType(Messages.Hired.class).firmID;
                        worker.addLink(firmID, Links.WorkerToFirmLink.class);
                        worker.status = Status.EMPLOYED;
                    }

                });
    }

    public static Action<Households> receiveSalary() {
        return Action.create(Households.class, worker -> {
            if (worker.hasMessageOfType(Messages.WorkerPayment.class)){
                worker.wealth += worker.getMessageOfType(Messages.WorkerPayment.class).wage;
            }
        });
    }

    public static Action<Households> AnnualCheck() {
        return Action.create(Households.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.AnnualCheck.class);
        });
    }

    public static Action<Households> CheckIfFired(){
        return Action.create(Households.class, worker -> {
           if (worker.hasMessageOfType(Messages.Fired.class)){
               worker.removeLinksTo(worker.getMessageOfType(Messages.Fired.class).getSender());
               worker.status = Status.UNEMPLOYED;
           }
        });
    }

}


