package macro_financial_framework.agents;

import macro_financial_framework.utils.Globals;
import macro_financial_framework.utils.Links;
import macro_financial_framework.MacroFinancialModel;
import macro_financial_framework.utils.Messages;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.HashMap;

public class Household extends Agent<Globals> {
    @Variable
    public int sector_skills;
    public boolean rich;
//    @Variable
//    public double accumulatedSalary;
    public double wealth;
    public double wage;
    public double unemploymentBenefits;
    @Variable
    public double productivity;
    public double consumptionBudget = 0;

    public enum Status {WORKER_EMPLOYED, WORKER_UNEMPLOYED, WORKER_UNEMPLOYED_APPLIED, INVESTOR}

    public Status status = Status.WORKER_UNEMPLOYED; //everyone starts by being unemployed
    public int lenghtOfUnemployment = 0;
    public HashMap<Integer, Double> budget;
    public int lenOfUnemployment = 0;

    public static Action<Household> ApplyForInvestor() {
        return Action.create(Household.class, investor -> {
            // determine who is an investor and not and connect the investors to firms
            investor.getLinks(Links.HouseholdToEconomy.class).send(Messages.ApplyForInvestor.class);
        });
    }

    public static Action<Household> DetermineStatus() {
        return Action.create(Household.class, household -> {
            // check if household has been assigned a firm and therefore status of investor
            if (household.hasMessageOfType(Messages.FirmAssignedToInvestor.class)) {
                household.status = Status.INVESTOR;
                household.addLink(household.getMessageOfType(Messages.FirmAssignedToInvestor.class).firmID, Links.InvestorToFirmLink.class);
            }
        });
    }

    public static Action<Household> applyForJob() {
        return Action.create(Household.class,
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

    public static Action<Household> updateAvailability() {
        return Action.create(Household.class, worker -> {
            if (worker.hasMessageOfType(Messages.Hired.class)) {
                long firmID = worker.getMessageOfType(Messages.Hired.class).firmID;
                worker.addLink(firmID, Links.WorkerToFirmLink.class);
                worker.status = Status.WORKER_EMPLOYED;
            }
        });
    }

    public static Action<Household> sendProductivity() {
        return Action.create(Household.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.Productivity.class, (productivityMessage, linkToFirm) -> {
                productivityMessage.productivity = worker.productivity;
            });
        });
    }

    public static Action<Household> receiveIncome() {
        return Action.create(Household.class, worker -> {
            if (worker.status == Status.WORKER_EMPLOYED && worker.hasMessageOfType(Messages.WorkerPayment.class)) {
                worker.wealth += worker.getMessageOfType(Messages.WorkerPayment.class).wage;
                worker.wage = worker.getMessageOfType(Messages.WorkerPayment.class).wage;
            } else if (worker.status == Status.WORKER_UNEMPLOYED || worker.status == Status.WORKER_UNEMPLOYED_APPLIED) {
                worker.wealth += worker.unemploymentBenefits;
            }
        });
    }


    public static Action<Household> sendDemand() {
        return Action.create(Household.class, worker -> {
            worker.budget.forEach((good, budget) -> {
                if (budget > 0) {
                    worker.send(Messages.HouseholdDemand.class, m -> {
                        m.consumptionBudget = budget;
                    }).to(worker.getGlobals().goodExchangeIDs.get(good));
                }
            });
        });
    }


    public static Action<Household> updateFromPurchase() {
        return Action.create(Household.class, household -> {
            if (household.hasMessageOfType(Messages.PurchaseCompleted.class)) {
                // the household saves the money that it hasn't used when purchasing
                household.wealth -= household.getMessageOfType(Messages.PurchaseCompleted.class).spent;
            }
        });
    }

    public static Action<Household> updateConsumptionBudget() {
        //update the consumption budget for each good after spending and receiving an income
        return Action.create(Household.class, household -> {
            // the new consumption budget for the next time period
            household.consumptionBudget = household.getGlobals().c * household.wealth;
            if (!household.rich) {
                // if the household is of common wealth it will only purchase all the goods except the exclusive ones
                int exclusiveGoods = (int) Math.ceil(0.2 * household.getGlobals().nbGoods);
                int goodsToPurchase = household.getGlobals().nbGoods - exclusiveGoods;
                double toSpend = household.consumptionBudget / goodsToPurchase;
                household.budget.clear();
                for (int i = 0; i < goodsToPurchase; i++) {
                    household.budget.put(i, toSpend);
                }
            } else {
                double toSpend = household.consumptionBudget / household.getGlobals().nbGoods;
                household.budget.clear();
                for (int i = 0; i < household.getGlobals().nbGoods; i++) {
                    household.budget.put(i, toSpend);
                }
            }
        });
    }


    public static Action<Household> getDividends() {
        return Action.create(Household.class, investor -> {
            if (investor.hasMessageOfType(Messages.PayInvestors.class)) {
                investor.wealth += investor.getMessageOfType(Messages.PayInvestors.class).dividend;
            }
        });
    }

    public static Action<Household> checkLengthOfUnemployment() {
        return Action.create(Household.class, worker -> {
            if (worker.status == Status.WORKER_UNEMPLOYED_APPLIED || worker.status == Status.WORKER_UNEMPLOYED) {
                worker.lenOfUnemployment += 1;
            }
        });
    }

    public static Action<Household> JobCheck() {
        return Action.create(Household.class, worker -> {
            worker.getLinks(Links.WorkerToFirmLink.class).send(Messages.JobCheck.class, (m, l) -> {
                m.productivity = worker.productivity;
            });
        });
    }

    public static Action<Household> CheckIfFired() {
        return Action.create(Household.class, worker -> {
            if (worker.hasMessageOfType(Messages.Fired.class)) {
                worker.removeLinksTo(worker.getMessageOfType(Messages.Fired.class).getSender());
                worker.status = Status.WORKER_UNEMPLOYED;
            }
        });
    }

    public static Action<Household> UnemployedWorkerCanApply() {
        return Action.create(Household.class, worker -> {
            if (worker.status == Status.WORKER_UNEMPLOYED_APPLIED) {
                worker.status = Status.WORKER_UNEMPLOYED;
            }
        });
    }

    public static Action<Household> UpgradeSkills() {
        return Action.create(Household.class, worker -> {
            // if the worker has been unemployed for a year or longer, the worker has a chance of upgrading its productivity
            if (worker.lenOfUnemployment >= 12) {
                double diff = 1.00 - worker.productivity; // a worker can´t have a productivity higher than one
                // there is a 50% chance of upgrading its skills
                worker.productivity = worker.productivity + (worker.getPrng().getNextInt(2) * worker.getPrng().getNextDouble(diff));
            }
        });
    }

    public static Action<Household> SendUnemployment() {
        return Action.create(Household.class, worker -> {
            if (worker.status == Status.WORKER_UNEMPLOYED || worker.status == Status.WORKER_UNEMPLOYED_APPLIED) {
                worker.getLinks(Links.HouseholdToEconomy.class).send(Messages.Unemployed.class, (unemploymentMessage, linkToEconomy) -> {
                    unemploymentMessage.sector = worker.sector_skills;
                });
            }
        });
    }

    public static Action<Household> ReviveFirm() {
        return Action.create(Household.class, investor -> {
            if (investor.hasMessageOfType(Messages.InvestorPaysRevival.class)) {
                investor.wealth -= investor.getMessageOfType(Messages.InvestorPaysRevival.class).debt;
            }
        });
    }

}


