package macro_financial_framework;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.*;

public class Economy extends Agent<MacroFinancialModel.Globals> {

    public List<WorkerID> availableWorkers;
    public List<FirmID> firmsHiring;
    public List<FirmSupplyInformation> firmsSupplyingGoods;
    public List<HouseholdDemandInformation> householdsDemandingGoods;
    public HashMap<Long, Double> priceOfGoods;
    public HashMap<Long, Double> firmWages;
    public HashMap<Long, Double> firmProductivity;


    public static Action<Economy> AssignInvestorToFirm() {
        return Action.create(Economy.class, market -> {
            List<Long> allHouseholds = new ArrayList<Long>();
            market.getMessagesOfType(Messages.ApplyForInvestor.class).forEach(msg -> {
                allHouseholds.add(msg.getSender());
            });

            List<Long> allFirms = new ArrayList<Long>();
            market.getMessagesOfType(Messages.FindInvestor.class).forEach(msg -> {
                allFirms.add(msg.getSender());
            });

            for (int i = 0; i < allFirms.size(); i++) {
                long investorID = allHouseholds.get(i);
                long firmID = allFirms.get(i);

                market.send(Messages.InvestorOfFirm.class, m -> {
                    m.investorID = investorID;
                }).to(firmID);

                market.send(Messages.FirmAssignedToInvestor.class, firm -> {
                    firm.firmID = firmID;
                }).to(investorID);

            }
        });
    }

    public static Action<Economy> SetFirmWages() {
        return Action.create(Economy.class, market -> {
            // creating a hashmap tp store all the sectors and their corresponding wage
            HashMap<Integer, Double> sectorWages = new HashMap<Integer, Double>();
            int numberOfSectors = market.getGlobals().nbSectors; // not adding -1 and instead keeping i<numberOfSector instead of <=
            for (int i = 0; i < numberOfSectors; i++) {
                int sector = i;
                double wage = market.getPrng().uniform(1000.00, 3000.00).sample();
                sectorWages.put(sector, wage);
            }

            market.getMessagesOfType(Messages.FirmInformation.class).forEach(m -> {
                int firmSector = m.sector;
                double firmWage = sectorWages.get(firmSector);
                market.send(Messages.FirmWage.class, msg -> {
                    msg.wage = firmWage;
                }).to(m.getSender());
            });
        });
    }

    public static Action<Economy> setFirmPriceOfGoods() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.priceOfGoods.class).forEach(msg -> {
                market.priceOfGoods.put(msg.getSender(), msg.price);
            });
        });
    }

    public static Action<Economy> setFirmPriceAndProductivity() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.FirmsPrice.class).forEach(msg -> {
                market.priceOfGoods.put(msg.getSender(), msg.price);
                market.firmProductivity.put(msg.getSender(), msg.productivity);
            });
        });
    }

    public static Action<Economy> setWages() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.Wages.class).forEach(msg -> {
                market.firmWages.put(msg.getSender(), msg.wage);
            });
        });
    }


    public static Action<Economy> MatchFirmsAndWorkers() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.JobApplication.class).forEach(msg -> {
                market.availableWorkers.add(new WorkerID(msg.getSender(), msg.sector, msg.productivity));
            });
            market.firmsHiring.clear();  // to account for new vacancies in case of firing
            market.getMessagesOfType(Messages.FirmInformation.class).forEach(mes -> {
                market.firmsHiring.add(new FirmID(mes.getSender(), mes.sector, mes.vacancies));
            });
            market.firmsHiring.forEach(firm -> {
                int sector = firm.sector;
                int vacancies = firm.vacancies;
                for (int x = 0; x < vacancies; x++) {
                    //TODO: choose worker with preferences of higher productivity
                    Optional<WorkerID> potentialWorkerGoodCandidate = market.availableWorkers.stream().filter(w -> w.sector == sector).findAny();
                    if (potentialWorkerGoodCandidate.isPresent()) {

                        // sends message to the workers
                        WorkerID worker = potentialWorkerGoodCandidate.get();
                        market.send(Messages.Hired.class, m -> {
                            m.firmID = firm.ID;
                        }).to(worker.ID);

                        // sends employee's info to firm
                        market.send(Messages.NewEmployee.class, e -> {
                            e.workerID = worker.ID;
                        }).to(firm.ID);

                        market.availableWorkers.remove(worker);
                        vacancies--;

                    } else {
                        //TODO: CHECK LOGIC TO SEE IF WE NEED TO UPDATE VACANCIES
//                        firm.vacancies = vacancies;
                        break;
                    }
                }

            });

        });
    }



    public static Action<Economy> sendDemandToFirm() {
        return Action.create(Economy.class, economy -> {
//            economy.supplyOfFirms.clear();
            economy.getMessagesOfType(Messages.FirmSupply.class).forEach(firmSupply -> {
                economy.firmsSupplyingGoods.add(new FirmSupplyInformation(firmSupply.getSender(), firmSupply.sector, firmSupply.output, firmSupply.price));
            });

//            economy.demandOfHousehold.clear();
            economy.getMessagesOfType(Messages.HouseholdDemand.class).forEach(householdDemand -> {
                economy.householdsDemandingGoods.add(new HouseholdDemandInformation(householdDemand.getSender(), householdDemand.sectorOfGoods, householdDemand.consumptionBudget));
            });


            // as of now just matching the good that the household is requesting to the sector of the firm
            // sending the demand for that good
            //TODO: households select good of lowest price
            economy.householdsDemandingGoods.forEach(household -> {

                Optional<FirmSupplyInformation> firmToPurchaseFrom = economy.firmsSupplyingGoods.stream().filter(firm -> firm.sectorOfFirm == household.sectorOfGoodsToPurchase).findAny();

                // sends a message to the firm with the ID of the household and the amount of goods it wants to purchase from that firm
                if (firmToPurchaseFrom.isPresent()) {
                    FirmSupplyInformation firmSupply = firmToPurchaseFrom.get();
                    int goodsDemanded = (int) Math.floor(household.consumptionBudget / firmSupply.price);
                    economy.send(Messages.HouseholdWantsToPurchase.class, messageOfHouseholdDemand -> {
                        messageOfHouseholdDemand.HouseholdID = household.ID;
                        messageOfHouseholdDemand.demand = goodsDemanded;
                    }).to(firmSupply.ID);
                }
            });
        });
    }
}
