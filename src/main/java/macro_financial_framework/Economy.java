package macro_financial_framework;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.graph.Link;

import java.util.*;

public class Economy extends Agent<MacroFinancialModel.Globals> {

    public List<WorkerID> availableWorkers;
    public List<FirmID> firmsHiring;
//    public List<FirmSupplyInformation> firmsSupplyingGoods;
//    public List<HouseholdDemandInformation> householdsDemandingGoods;
    public HashMap<Double, Double> priceOfGoods;
//    public HashMap<Long, Double> firmWages;
//    public HashMap<Long, Double> firmProductivity;
    // this is to calculate the average price -> TODO: find a better way of doing this
    private double numerator = 0; // this is the sum of the product of the price and output for each firm
    private double denominator = 0; // this is the sum of the output of every firm
    public double averagePrice;
    @Variable
    public int unemployment = 0;




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
                double wage = market.getPrng().uniform(2000.00, 4000.00).sample();
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

    public static Action<Economy> setFirmGood() {
        return Action.create(Economy.class, market -> {
            // creating a hashmap tp store all the sectors and their corresponding good traded
            HashMap<Integer, Integer> sectorGood = new HashMap<Integer, Integer>();
            int numberOfGoods = market.getGlobals().nbGoods; // not adding -1 and instead keeping i<numberOfSector instead of <=
            int numberOfSectors = market.getGlobals().nbSectors;
            for (int i = 0; i < numberOfSectors; i++) {
                int sector = i;
                int good = market.getPrng().getNextInt(numberOfGoods);
                sectorGood.put(sector, good);
            }

            market.getMessagesOfType(Messages.FirmInformation.class).forEach(m -> {
                int firmSector = m.sector;
                int goodTraded = sectorGood.get(firmSector);
                market.send(Messages.FirmGood.class, msg -> {
                    msg.good = goodTraded;
                }).to(m.getSender());
            });
        });
    }

    public static Action<Economy> GetPrices() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.FirmsPrice.class).forEach(priceMessage -> {
                market.priceOfGoods.put(priceMessage.price, priceMessage.output);
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
                    market.availableWorkers.sort(Comparator.comparing(worker -> worker.productivity));
                    Optional<WorkerID> potentialWorkerGoodCandidate = market.availableWorkers.stream().filter(w -> w.sector == sector).findFirst();
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



//    public static Action<Economy> sendDemandToFirm() {
//        return Action.create(Economy.class, economy -> {
////            economy.supplyOfFirms.clear();
//            economy.getMessagesOfType(Messages.FirmSupply.class).forEach(firmSupply -> {
//                economy.firmsSupplyingGoods.add(new FirmSupplyInformation(firmSupply.getSender(), firmSupply.sector, firmSupply.output, firmSupply.price));
//            });
//
////            economy.demandOfHousehold.clear();
//            economy.getMessagesOfType(Messages.HouseholdDemand.class).forEach(householdDemand -> {
//                economy.householdsDemandingGoods.add(new HouseholdDemandInformation(householdDemand.getSender(), householdDemand.sectorOfGoods, householdDemand.consumptionBudget));
//            });
//
//
//            // as of now just matching the good that the household is requesting to the sector of the firm
//            // sending the demand for that good
//            //TODO: households select good of lowest price
//            economy.householdsDemandingGoods.forEach(household -> {
//
//                Optional<FirmSupplyInformation> firmToPurchaseFrom = economy.firmsSupplyingGoods.stream().filter(firm -> firm.sectorOfFirm == household.sectorOfGoodsToPurchase).findAny();
//
//                // sends a message to the firm with the ID of the household and the amount of goods it wants to purchase from that firm
//                if (firmToPurchaseFrom.isPresent()) {
//                    FirmSupplyInformation firmSupply = firmToPurchaseFrom.get();
//                    int goodsDemanded = (int) Math.floor(household.consumptionBudget / firmSupply.price);
//                    economy.send(Messages.HouseholdWantsToPurchase.class, messageOfHouseholdDemand -> {
//                        messageOfHouseholdDemand.HouseholdID = household.ID;
//                        messageOfHouseholdDemand.demand = goodsDemanded;
//                    }).to(firmSupply.ID);
//                }
//            });
//        });
//    }

    public static Action<Economy> calculateUnemployment() {
        return Action.create(Economy.class, market -> {
            if(market.hasMessageOfType(Messages.Unemployed.class)){
                market.getMessagesOfType(Messages.Unemployed.class).forEach( msg ->
                        market.unemployment += 1);
            }

            // send the current unemployment to the firms -> available workers
            market.getLinks(Links.EconomyToFirm.class).send(Messages.CurrentUnemployment.class, (unemploymentMessage, linkToFirms) -> {
                unemploymentMessage.unemployment = market.unemployment;
            });
        });
    }

    public static Action<Economy> CalculateAndSendAveragePrice() {
        return Action.create(Economy.class, market -> {
            market.priceOfGoods.forEach((price, output) -> {
                market.numerator += (price * output);
                market.denominator += output;
            });
            market.averagePrice = market.numerator / market.denominator;
            market.getLinks(Links.EconomyToFirm.class).send(Messages.AveragePrice.class, market.averagePrice);
        });
    }

}
