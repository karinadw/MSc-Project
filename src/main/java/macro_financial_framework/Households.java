package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.HashMap;

public class Households extends Agent<MacroFinancialModel.Globals> {
    @Variable
    public int sector_skills;
    public boolean rich;
    @Variable
    public double accumulatedSalary;
    public double savings; // their initial wealth

    public double wealth = accumulatedSalary + savings;
    public double wage;
    public double unemploymentBenefits;
    @Variable
    public double productivity;
    public double consumptionBudget = 0;

    public enum Status {WORKER_EMPLOYED, WORKER_UNEMPLOYED, WORKER_UNEMPLOYED_APPLIED, INVESTOR}

    public Status status = Status.WORKER_UNEMPLOYED; //everyone starts by being unemployed
    public HashMap<Integer, Double> budget;

    public static Action<Households> ApplyForInvestor() {
        return Action.create(Households.class, investor -> {
            // determine who is an investor and not and connect the investors to firms
            investor.getLinks(Links.HouseholdToEconomy.class).send(Messages.ApplyForInvestor.class);
        });
    }

    public static Action<Households> DetermineStatus() {
        return Action.create(Households.class, household -> {
            // check if household has been assigned a firm and therefore status of investor
            if (household.hasMessageOfType(Messages.FirmAssignedToInvestor.class)) {
                household.status = Status.INVESTOR;
                household.addLink(household.getMessageOfType(Messages.FirmAssignedToInvestor.class).firmID, Links.InvestorToFirmLink.class);
            }
        });
    }

    public static Action<Households> applyForJob() {
        return Action.create(Households.class,
                worker -> {
                    if (worker.status == Status.WORKER_UNEMPLOYED) {
                        worker.getLinks(Links.HouseholdToEconomy.class).send(Messages.JobApplication.class, (msg, link) -> {
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
//                        worker.send(Messages.Productivity.class, m -> {
//                            m.productivity = worker.productivity;
//                        }).to(firmID); //sends productivity to the firm its working for
                        worker.status = Status.WORKER_EMPLOYED;
                    }

                });
    }

    public static Action<Households> sendProductivity() {
        return Action.create(Households.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.Productivity.class, (m, l) -> {
                m.productivity = worker.productivity;
            });
        });
    }

    public static Action<Households> receiveIncome() {
        return Action.create(Households.class, worker -> {
            if (worker.status == Status.WORKER_EMPLOYED && worker.hasMessageOfType(Messages.WorkerPayment.class)) {
                worker.accumulatedSalary += worker.getMessageOfType(Messages.WorkerPayment.class).wage;
                worker.wage = worker.getMessageOfType(Messages.WorkerPayment.class).wage;
            } else if (worker.status == Status.WORKER_UNEMPLOYED || worker.status == Status.WORKER_UNEMPLOYED_APPLIED) {
                worker.accumulatedSalary += worker.unemploymentBenefits;
            }
        });
    }


    public static Action<Households> sendDemand() {
        return Action.create(Households.class, worker -> {
            worker.budget.forEach((good, budget) -> {
                worker.send(Messages.HouseholdDemand.class, m -> {
                    m.consumptionBudget = budget;
                }).to(worker.getGlobals().goodExchangeIDs.get(good));
            });
        });
    }


    public static Action<Households> updateFromPurchase() {
        return Action.create(Households.class, household -> {
            if (household.hasMessageOfType(Messages.PurchaseCompleted.class)) {
                household.wealth -= household.getMessageOfType(Messages.PurchaseCompleted.class).spent;

                // keeping track of the total consumption budget of the households
                household.budget.forEach((good, budget) -> household.consumptionBudget += budget);

                // the household saves the money that it hasn't used when purchasing
                household.savings = household.consumptionBudget - household.getMessageOfType(Messages.PurchaseCompleted.class).spent;
            }
        });
    }

    public static Action<Households> updateConsumptionBudget() {
        //TODO: update this method for any number of goods
        //update the consumption budget for each good after spending and receiving an income
        return Action.create(Households.class, household -> {
            if (!household.rich) {
                // if the household is of common wealth it will only purchase competitive goods, as of now these are 2
                double toSpend = household.consumptionBudget / 2;
                household.budget.clear();
                for (int i = 0; i < 2; i++) {
                    household.budget.put(i, toSpend);
                }
            } else {
                double toSpend = household.consumptionBudget / 3;
                household.budget.clear();
                for (int i = 0; i <= 2; i++) {
                    household.budget.put(i, toSpend);
                }
            }
        });
    }


    public static Action<Households> getDividends() {
        return Action.create(Households.class, investor -> {
            if (investor.hasMessageOfType(Messages.PayInvestors.class)) {
                investor.accumulatedSalary += investor.getMessageOfType(Messages.PayInvestors.class).dividend;
            }
        });
    }

    public static Action<Households> JobCheck() {
        return Action.create(Households.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.JobCheck.class, (m, l) -> {
                m.productivity = worker.productivity;
            });
        });
    }

    public static Action<Households> CheckIfFired() {
        return Action.create(Households.class, worker -> {
            if (worker.hasMessageOfType(Messages.Fired.class)) {
                worker.removeLinksTo(worker.getMessageOfType(Messages.Fired.class).getSender());
                worker.status = Status.WORKER_UNEMPLOYED;
            }
        });
    }

    public static Action<Households> UnemployedWorkerCanApply() {
        return Action.create(Households.class, worker -> {
            if (worker.status == Status.WORKER_UNEMPLOYED_APPLIED){
                worker.status = Status.WORKER_UNEMPLOYED;
            }
        });
    }

    public static Action<Households> SendUnemployment() {
        return Action.create(Households.class, worker -> {
           if (worker.status == Status.WORKER_UNEMPLOYED || worker.status == Status.WORKER_UNEMPLOYED_APPLIED){
               worker.getLinks(Links.HouseholdToEconomy.class).send(Messages.Unemployed.class, (unemploymentMessage, linkToEconomy) -> {
                   unemploymentMessage.sector = worker.sector_skills;
               });
           }
        });
    }

}


