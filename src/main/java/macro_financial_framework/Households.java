package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Households extends Agent<MacroFinancialModel.Globals> {
    @Variable
    public int sector_skills;
    @Variable
    public double savings;
    public double wage;
    public double unemploymentBenefits;
    @Variable
    public double productivity;
    public enum Status {WORKER_EMPLOYED, WORKER_UNEMPLOYED, WORKER_UNEMPLOYED_APPLIED, INVESTOR}

    public Status status = Status.WORKER_UNEMPLOYED; //everyone starts by being unemployed

    public static Action<Households> ApplyForInvestor() {
        return Action.create(Households.class, investor -> {
            // determine who is an investor and not and connect the investors to firms
            investor.getLinks(Links.WorkerToEconomyLink.class).send(Messages.ApplyForInvestor.class);
        });
    }

    public static Action<Households> DetermineStatus() {
        return Action.create(Households.class, household -> {
            // check if household has been assigned a firm and therefore status of investor
           if (household.hasMessageOfType(Messages.FirmAssignedToInvestor.class)){
               household.status = Status.INVESTOR;
               household.addLink(household.getMessageOfType(Messages.FirmAssignedToInvestor.class).firmID, Links.InvestorToFirmLink.class);
            }
        });
    }
    public static Action<Households> applyForJob() {
        return Action.create(Households.class,
                worker -> {
                    if (worker.status == Status.WORKER_UNEMPLOYED) {
                        worker.getLinks(Links.WorkerToEconomyLink.class).send(Messages.JobApplication.class, (msg, link) -> {
                            msg.productivity = worker.productivity;
                            msg.sector = worker.sector_skills;
                        });
                        worker.status = Status.WORKER_UNEMPLOYED_APPLIED;
                    }
                });
    }

    public static Action<Households> updateAvailability() {
        return Action.create(Households.class,
                worker -> {
                    if (worker.hasMessageOfType(Messages.Hired.class)) {
                        long firmID = worker.getMessageOfType(Messages.Hired.class).firmID;
                        worker.addLink(firmID, Links.WorkerToFirmLink.class);
                        worker.send(Messages.Productivity.class, m -> {m.productivity = worker.productivity;}).to(firmID); //sends productivity to the firm its working for
                        worker.status = Status.WORKER_EMPLOYED;
                    }

                });
    }

    public static Action<Households> sendProductivity(){
        return Action.create(Households.class, worker -> {
           worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.Productivity.class, (m, l) -> {
               m.productivity = worker.productivity;
           });
        });
    }

    public static Action<Households> receiveIncome() {
        return Action.create(Households.class, worker -> {
            if (worker.status == Status.WORKER_EMPLOYED && worker.hasMessageOfType(Messages.WorkerPayment.class)){
                worker.savings += worker.getMessageOfType(Messages.WorkerPayment.class).wage;
                worker.wage = worker.getMessageOfType(Messages.WorkerPayment.class).wage;
            } else if (worker.status == Status.WORKER_UNEMPLOYED || worker.status == Status.WORKER_UNEMPLOYED_APPLIED){
                worker.savings += worker.unemploymentBenefits;
            }
        });
    }

    public static Action<Households> sendDemand() {
        return Action.create(Households.class, worker -> {
            worker.getLinks(Links.WorkerToEconomyLink.class).send(Messages.HouseholdDemand.class, (m, l) -> {
                m.consumptionBudget = worker.getGlobals().c * (worker.savings + worker.wage);
                m.sectorOfGoods = worker.getPrng().getNextInt(worker.getGlobals().nbSectors - 1); // random sector to consume from
            });
        });
    }

    public static Action<Households> AnnualCheck() {
        return Action.create(Households.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.AnnualCheck.class, (m, l) -> {
                m.productivity = worker.productivity;
            });
        });
    }

    public static Action<Households> CheckIfFired(){
        return Action.create(Households.class, worker -> {
           if (worker.hasMessageOfType(Messages.Fired.class)){
               worker.removeLinksTo(worker.getMessageOfType(Messages.Fired.class).getSender());
               worker.status = Status.WORKER_UNEMPLOYED;
           }
        });
    }

}


