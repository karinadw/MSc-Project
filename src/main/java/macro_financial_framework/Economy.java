package macro_financial_framework;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.*;

public class Economy extends Agent<MacroFinancialModel.Globals> {

    public List<WorkerID> availableWorkers;
    public List<FirmID> firmsHiring;
    //    public List<FirmSupplyInformation> firmsSupplyingGoods;
//    public List<HouseholdDemandInformation> householdsDemandingGoods;
    public HashMap<Double, Double> priceOfGoods;

    public HashMap<Long, HealthyFirmAccount> healthyFirmAccountMap;
    //    public HashMap<Long, Double> firmWages;
//    public HashMap<Long, Double> firmProductivity;
    // this is to calculate the average price -> TODO: find a better way of doing this
    private double numerator = 0; // this is the sum of the product of the price and output for each firm
    private double denominator = 0; // this is the sum of the output of every firm
    public double averagePrice;
    public double previousAveragePrice;
    @Variable
    public double inflation;
    @Variable
    public int unemployment = 0;
    public double deficit = 0;
    public HashMap<Long, Double> indebtedFirmsMap;
    public ArrayList<Long> bankruptFirmsArray;
    public HashMap<Long, BailoutPackage> bailedOutFirmsMap;


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

    public static Action<Economy> SetFirmProperties() {
        // sets the sector specific goods
        // sets the sector specific wages
        // sends these to the firms
        return Action.create(Economy.class, market -> {
            // creating a hashmap tp store all the sectors and their corresponding wage
            HashMap<Integer, Double> sectorWages = new HashMap<Integer, Double>();
            HashMap<Integer, Integer> sectorGood = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> sectorGoodToPurchase = new HashMap<Integer, Integer>();
            int numberOfSectors = market.getGlobals().nbSectors; // not adding -1 and instead keeping i<numberOfSector instead of <=
            int numberOfGoods = market.getGlobals().nbGoods; // not adding -1 and instead keeping i<numberOfSector instead of <=
            for (int i = 0; i < numberOfSectors; i++) {
                int sector = i;
                // TODO: check if the numbers for the wages makes sense
                double wage = market.getPrng().uniform(2000.00, 4000.00).sample();
                sectorWages.put(sector, wage);
                // int good = market.getPrng().getNextInt(numberOfGoods);
                int good = sector;
                // TODO: check if assumption below is fine
                // Setting one good per sector, obvs this is a complex way of doing it but it is left like this in case the assumption is not valid
                sectorGood.put(sector, good);

//                int goodNeededForProduction = market.getPrng().getNextInt(numberOfGoods);
//                // I've just put greater than 0 as a placeholder
//                // the loop will keep going if the good to purchase in that sector and the one being produced are the same
//                // when they are not there is a break statement
//                while (goodNeededForProduction >= 0){
//                    // if the good of that sector and the good needed for production are the same then recalculate
//                    if (goodNeededForProduction == good){
//                        goodNeededForProduction = market.getPrng().getNextInt(numberOfGoods);
//                    } else if (goodNeededForProduction != good){
//                        sectorGoodToPurchase.put(sector, goodNeededForProduction);
//                        break;
//                    }
//                }

                // easier way to do the above -> this was all firms has a connection with another firm for intermediate goods
                int goodNeededForProduction = (i+1) % numberOfGoods;
                sectorGoodToPurchase.put(sector, goodNeededForProduction);
            }

            market.getMessagesOfType(Messages.FirmInformation.class).forEach(m -> {
                int firmSector = m.sector;
                double firmWage = sectorWages.get(firmSector);
                int goodTraded = sectorGood.get(firmSector);
                int goodToPurchase = sectorGoodToPurchase.get(firmSector);
                market.send(Messages.FirmProperties.class, msg -> {
                    msg.wage = firmWage;
                    msg.good = goodTraded;
                    msg.goodToPurchase = goodToPurchase;
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

    public static Action<Economy> CalculateAndSendAveragePrice() {
        return Action.create(Economy.class, market -> {
            market.priceOfGoods.forEach((price, output) -> {
                market.numerator += (price * output);
                market.denominator += output;
            });

            // stores the previous average price to calculate inflation
            market.previousAveragePrice = market.averagePrice;
//            System.out.println("average price " + market.averagePrice + " previous average price " + market.previousAveragePrice);

            // new average price
            market.averagePrice = market.numerator / market.denominator;
            market.getLinks(Links.EconomyToFirm.class).send(Messages.AveragePrice.class,  (m, l) -> {
                m.averagePrice = market.averagePrice;
            });
        });
    }

    public static Action<Economy> CalculateInflation() {
        return Action.create(Economy.class, market -> {
            market.inflation = (market.averagePrice - market.previousAveragePrice) / market.previousAveragePrice;
            market.inflation = market.inflation / market.getGlobals().gamma_p;
        });

    }

    public static Action<Economy> MatchFirmsAndWorkers() {
        return Action.create(Economy.class, market -> {

            market.availableWorkers.clear();
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
                    market.availableWorkers.sort(Comparator.comparing(worker -> worker.productivity));
                    Collections.reverse(market.availableWorkers);
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
                        break;
                    }
                }

            });

        });
    }

    public static Action<Economy> calculateUnemploymentAndAavailableWorkers() {
        return Action.create(Economy.class, market -> {
            if (market.hasMessageOfType(Messages.Unemployed.class)) {
                market.getMessagesOfType(Messages.Unemployed.class).forEach(msg ->
                        market.unemployment += 1);
            }

            // send the current unemployment to the firms -> available workers
            market.getLinks(Links.EconomyToFirm.class).send(Messages.CurrentUnemployment.class, (unemploymentMessage, linkToFirms) -> {
                unemploymentMessage.unemployment = market.unemployment;
            });

            HashMap<Long, Integer> availableWorkers = new HashMap<Long, Integer>();
            if (market.hasMessageOfType(Messages.Unemployed.class)) {
                market.getMessagesOfType(Messages.Unemployed.class).forEach(msg ->
                        availableWorkers.put(msg.getSender(), msg.sector)
                );
            }

            // store the sector and the total available workers per sector
            HashMap<Integer, Integer> availableWorkersPerSector = new HashMap<Integer, Integer>();
            for(int sector = 0; sector < market.getGlobals().nbSectors; sector++){
                int workers = Collections.frequency(availableWorkers.values(), sector);
                availableWorkersPerSector.put(sector, workers);
            }

            // for each message from the firm, the economy sends a message back to the firm with the available workers in its sector
            if (market.hasMessageOfType(Messages.FirmGetAvailableWorkers.class)){
                market.getMessagesOfType(Messages.FirmGetAvailableWorkers.class).forEach(message -> {
                    market.send(Messages.AvailableWorkersInYourSector.class, totalAvailableWorkers -> {
                        totalAvailableWorkers.workers = availableWorkersPerSector.get(message.sector);
                    }).to(message.getSender());
                });
            }
        });
    }

    //TODO: check this code when simulation works
    //TODO: there is probably a more efficient way of doing this
//    public static Action<Economy> calculateAvailableWorkers() {
//        // store all the IDs of the workers and their respective sector
//        return Action.create(Economy.class, market -> {
//            HashMap<Long, Integer> availableWorkers = new HashMap<Long, Integer>();
//            if (market.hasMessageOfType(Messages.Unemployed.class)) {
//                market.getMessagesOfType(Messages.Unemployed.class).forEach(msg ->
//                        availableWorkers.put(msg.getSender(), msg.sector)
//                );
//            }
//
//            // store the sector and the total available workers per sector
//            HashMap<Integer, Integer> availableWorkersPerSector = new HashMap<Integer, Integer>();
//            for(int sector = 0; sector < market.getGlobals().nbSectors; sector++){
//                int workers = Collections.frequency(availableWorkers.values(), sector);
//                availableWorkersPerSector.put(sector, workers);
//            }
//
//            // for each message from the firm, the economy sends a message back to the firm with the available workers in its sector
//            if (market.hasMessageOfType(Messages.FirmGetAvailableWorkers.class)){
//                market.getMessagesOfType(Messages.FirmGetAvailableWorkers.class).forEach(message -> {
//                    market.send(Messages.AvailableWorkersInYourSector.class, totalAvailableWorkers -> {
//                        totalAvailableWorkers.sector = availableWorkersPerSector.get(message.sector);
//                    }).to(message.getSender());
//                });
//            }
//        });
//    }

    public static Action<Economy> receiveHealthyFirmAccounts() {
        return Action.create(Economy.class, economy -> {
            economy.healthyFirmAccountMap.clear();
            economy.getMessagesOfType(Messages.HealthyFirmAccountMessage.class).forEach(msg -> {
                economy.healthyFirmAccountMap.put(msg.getSender(), msg.healthyFirmAccount);
            });
        });
    }

    public static Action<Economy> receiveIndebtedFirmDebt() {
        return Action.create(Economy.class, economy -> {
            economy.indebtedFirmsMap.clear();
            economy.getMessagesOfType(Messages.BailoutRequestMessage.class).forEach(msg -> {
                economy.indebtedFirmsMap.put(msg.getSender(), msg.debt);
            });
        });
    }

    public static Action<Economy> checkDefaults() {
        return Action.create(Economy.class, economy -> {
            economy.indebtedFirmsMap.forEach((indebtedFirmID, debt) -> {
                if (economy.healthyFirmAccountMap.size() > 0) {

                    // list of IDs of the healthy firms
                    ArrayList<Long> healthyFirmIDs = new ArrayList<>(economy.healthyFirmAccountMap.keySet());

                    // chooses a random healthy firm
                    int idx = economy.getPrng().generator.nextInt(economy.healthyFirmAccountMap.size());
                    long healthyFirmID = healthyFirmIDs.get(idx);

                    // first condition is the random probability of the healthy firm acquiring the indebted firm
                    if ((economy.getPrng().uniform(0, 1).sample() < 1 - economy.getGlobals().f) & (economy.healthyFirmAccountMap.get(healthyFirmID).deposits > -debt)) {

                        // the healthy firm pays off the debt of the indebted firm
                        economy.healthyFirmAccountMap.get(healthyFirmID).updateDeposits(debt);
                        economy.send(Messages.PaidDebtOfIndebtedFirm.class, paymentOdDebtMessage -> {
                           paymentOdDebtMessage.debt = debt;
                        }).to(healthyFirmID);

                        // the indebted firm no longer has any debt
                        // not too sure if I need to do this in this step
                        // economy.send(Messages.NoDebt.class).to(indebtedFirmID);
                        economy.bailedOutFirmsMap.put(indebtedFirmID, new BailoutPackage(economy.healthyFirmAccountMap.get(healthyFirmID).price, economy.healthyFirmAccountMap.get(healthyFirmID).wage));
                    }
                    else {
                        economy.deficit -= debt;
                        economy.bankruptFirmsArray.add(indebtedFirmID);
                    }
                }
                else {
                    economy.deficit -= debt;
                    economy.bankruptFirmsArray.add(indebtedFirmID);
                }
            });
            economy.indebtedFirmsMap.clear();
        });
    }


    public static Action<Economy> sendBailoutPackages() {
        return Action.create(Economy.class, economy -> {
            economy.bailedOutFirmsMap.forEach((firmID, bailoutPackage) -> {
                economy.getLinksTo(firmID, Links.EconomyToFirm.class).send(Messages.BailoutPackageMessage.class, (msg, link) -> {
                    msg.price = bailoutPackage.price;
                    msg.wage = bailoutPackage.wage;
                });
            });
            economy.bailedOutFirmsMap.clear();
        });
    }

    public static Action<Economy> sendBankruptcyMessages() {
        return Action.create(Economy.class, economy -> {
            economy.bankruptFirmsArray.forEach(firmID -> {
                economy.getLinksTo(firmID, Links.EconomyToFirm.class).send(Messages.BankruptcyMessage.class);
            });
            economy.bankruptFirmsArray.clear();
        });
    }
}
