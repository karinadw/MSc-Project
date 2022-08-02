package SimpleEconomyModel.agents;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;

import SimpleEconomyModel.utils.*;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.*;

public class Economy extends Agent<Globals> {

    @Variable
    public double inflation;
    @Variable
    public double unemployment;
    @Variable
    public double employment;
    public List<WorkerID> availableWorkers;
    public List<FirmID> firmsHiring;
    public HashMap<Double, Double> priceOfGoods;
    public HashMap<Double, Double> priceOfGoodsDemanded;
    public HashMap<Long, HealthyFirmAccount> healthyFirmAccountMap;
    public double numerator; // this is the sum of the product of the price and output for each firm
    public double denominator; // this is the sum of the output of every firm
    public double averagePrice;
    public double previousAveragePrice;
    public double deficit;
    public HashMap<Long, Double> indebtedFirmsMap;
    public ArrayList<Long> bankruptFirmsArray;
    public HashMap<Long, BailoutPackage> bailedOutFirmsMap;

    public static Action<Economy> InitWeights() {
        return Action.create(Economy.class, economy -> {

            // empirical data from the austrian economy -> 62x62 matrix collapse into 10x10 matrix
            // sum of all the rows should equal one
            double a[][] = {{0.446077143, 0.332209946, 0.054489028, 0.065645351, 0.004974503, 0.025114853, 0.01029264, 0.058379932, 0.001662539, 0.001154064},
                    {0.026752214, 0.698613993, 0.029356368, 0.120582545, 0.018077628, 0.021997578, 0.012313881, 0.068505531, 0.001803847, 0.001996415},
                    {0.000258603, 0.371519029, 0.380510689, 0.078452974, 0.003745658, 0.017886715, 0.020984379, 0.124729182, 0.000975335, 0.000937436},
                    {0.00306286, 0.219450082, 0.039462672, 0.408299033, 0.025581603, 0.039316993, 0.090062361, 0.163416189, 0.003437634, 0.007910574},
                    {7.27588E-06, 0.208309968, 0.010395399, 0.052818424, 0.444684707, 0.026364472, 0.048159009, 0.170233117, 0.00187896, 0.037148669},
                    {1.79544E-05, 0.031672296, 0.011619596, 0.058637984, 0.089736979, 0.551249182, 0.033186157, 0.213027859, 0.006717462, 0.00413453},
                    {0.000210885, 0.237516128, 0.260010802, 0.010885532, 0.010657744, 0.132664404, 0.201939338, 0.145022912, 0.000390379, 0.000701877},
                    {0.001257607, 0.103868827, 0.014023935, 0.142682885, 0.086673445, 0.055527533, 0.055759544, 0.529754306, 0.005200104, 0.005251815},
                    {0.002763084, 0.313877838, 0.075627115, 0.118724354, 0.036005715, 0.066325106, 0.145184494, 0.157118542, 0.072541413, 0.01183234},
                    {0.000277457, 0.230205887, 0.070512975, 0.131287762, 0.043116028, 0.05831017, 0.121092903, 0.230302527, 0.003223874, 0.111670418}
            };
            economy.getGlobals().weightsArray = a;
        });
    }

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

            Collections.shuffle(allHouseholds, market.getPrng().getRandom());
            Collections.shuffle(allFirms, market.getPrng().getRandom());

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

    public static Action<Economy> GetPrices() {
        return Action.create(Economy.class, market -> {
            market.priceOfGoods.clear();
            market.getMessagesOfType(Messages.FirmsPrice.class).forEach(priceMessage -> {
                market.priceOfGoods.put(priceMessage.price, priceMessage.output);
                //System.out.println("Received " + priceMessage.output + " units of good at a price of " + priceMessage.price + " from firm " + priceMessage.getSender());
            });
            market.priceOfGoodsDemanded.clear();
            market.getMessagesOfType(Messages.FirmsPriceDemandedGoods.class).forEach(priceMessage -> {
                market.priceOfGoods.put(priceMessage.price, priceMessage.output);
                //System.out.println("Received " + priceMessage.output + " units of good at a price of " + priceMessage.price + " from firm " + priceMessage.getSender());
            });
        });
    }

//    public static Action<Economy> ReceiveGoodsSold() {
//        return Action.create(Economy.class, market -> {
//            market.numerator = 0;
//            market.denominator = 0;
//
//            // stores the previous average price to calculate inflation
//            market.getMessagesOfType(Messages.SoldGoodsAndPrices.class).forEach(msg -> {
//                market.numerator += (msg.price * msg.quantitySold);
//                market.denominator += msg.quantitySold;
//                System.out.println("numerator " + market.numerator);
//                System.out.println("denominator " + market.denominator);
//            });
//        });
//    }

//    public static Action<Economy> ReceiveGoodsSold() {
//        return Action.create(Economy.class, market -> {
//            market.numerator = 0;
//            market.denominator = 0;
//            // stores the previous average price to calculate inflation
//            market.getMessagesOfType(Messages.SoldGoodsAndPrices.class).forEach(msg -> {
//                market.numerator += (msg.price * msg.quantitySold);
//                market.denominator += msg.quantitySold;
//                System.out.println("numerator " + market.numerator);
//                System.out.println("denominator " + market.denominator);
//            });
//        });
//    }

    public static Action<Economy> CalculateAndSendAveragePrice() {
        return Action.create(Economy.class, market -> {
            market.numerator = 0;
            market.denominator = 0;
            market.priceOfGoods.forEach((price, output) -> {
                market.numerator += (price * output);
                market.denominator += output;
            });

//            System.out.println("numerator " + market.numerator + "denominator " + market.denominator);

            // stores the previous average price to calculate inflation
            market.previousAveragePrice = market.averagePrice;

            // new average price
            market.averagePrice = market.numerator / market.denominator;
            System.out.println("previous average price " + market.previousAveragePrice + " current average price " + market.averagePrice);

            market.numerator = 0;
            market.denominator = 0;
            market.priceOfGoodsDemanded.forEach((price, output) -> {
                market.numerator += (price * output);
                market.denominator += output;
            });
            // average price of demanded goods
            market.getLinks(Links.EconomyToFirm.class).send(Messages.AveragePrice.class, (m, l) -> {
                m.averagePrice = market.numerator/ market.denominator;
            });
        });
    }


    public static Action<Economy> CalculateInflation() {
        return Action.create(Economy.class, market -> {
            market.inflation = (market.averagePrice - market.previousAveragePrice) / market.previousAveragePrice;
            //System.out.println(market.inflation+2);
//            market.inflation = (market.inflation / market.getGlobals().gamma_p) + 2;
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

            Collections.shuffle(market.availableWorkers, market.getPrng().getRandom());
            Collections.shuffle(market.firmsHiring, market.getPrng().getRandom());

            market.firmsHiring.forEach(firm -> {
                int sector = firm.sector;
                int vacancies = firm.vacancies;

                while (vacancies > 0) {

                    // sort from high to low productivity
                    market.availableWorkers.sort(Comparator.comparing(worker -> worker.productivity));
                    Collections.reverse(market.availableWorkers);
                    Optional<WorkerID> potentialWorker = market.availableWorkers.stream().filter(w -> w.sector == sector).findFirst();
                    if (potentialWorker.isPresent()) {

                        // sends message to the workers
                        WorkerID worker = potentialWorker.get();
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

    public static Action<Economy> calculateUnemploymentAndAvailableWorkers() {
        return Action.create(Economy.class, market -> {

            market.unemployment = 0;
            market.employment = 0;
            market.getMessagesOfType(Messages.Unemployed.class).forEach(msg ->
                    market.unemployment += 1);
            market.getMessagesOfType(Messages.Employed.class).forEach(employed ->
                    market.employment += 1);

            //market.unemployment /= market.getGlobals().nbWorkers;
            market.unemployment = (market.unemployment / (double) market.getGlobals().nbWorkers) * 100;
            market.employment = (market.employment / (double) market.getGlobals().nbWorkers) * 100;

//             //for debugging purposes
//            System.out.println("Unemployment " + market.unemployment);
//            System.out.println("Employment " + market.employment);
//            System.out.println(market.getGlobals().nbWorkers);
//            System.out.println(market.employment + market.unemployment + market.getGlobals().nbFirms);

            // send the current unemployment to the firms
            market.getLinks(Links.EconomyToFirm.class).send(Messages.CurrentUnemployment.class, (unemploymentMessage, linkToFirms) -> {
                unemploymentMessage.unemployment = market.unemployment;
            });


            market.availableWorkers.clear();
            HashMap<Long, Integer> availableWorkers = new HashMap<Long, Integer>();
            if (market.hasMessageOfType(Messages.Unemployed.class)) {
                market.getMessagesOfType(Messages.Unemployed.class).forEach(msg ->
                        availableWorkers.put(msg.getSender(), msg.sector)
                );
            }

            // store the sector and the total available workers per sector
            HashMap<Integer, Integer> availableWorkersPerSector = new HashMap<Integer, Integer>();
            for (int sector = 0; sector < market.getGlobals().nbSectors; sector++) {
                int workers = Collections.frequency(availableWorkers.values(), sector);
                availableWorkersPerSector.put(sector, workers);
            }

            // for each message from the firm, the economy sends a message back to the firm with the available workers in its sector
            if (market.hasMessageOfType(Messages.FirmGetAvailableWorkers.class)) {
                market.getMessagesOfType(Messages.FirmGetAvailableWorkers.class).forEach(message -> {
                    market.send(Messages.AvailableWorkersInYourSector.class, totalAvailableWorkers -> {
                        totalAvailableWorkers.workers = availableWorkersPerSector.get(message.sector);
                    }).to(message.getSender());
                });
            }
        });
    }

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
                    if ((economy.getPrng().uniform(0, 1).sample() < 1 - economy.getGlobals().f) && (economy.healthyFirmAccountMap.get(healthyFirmID).deposits > -debt)) {

                        // the healthy firm pays off the debt of the indebted firm
                        economy.healthyFirmAccountMap.get(healthyFirmID).updateDeposits(debt);
                        economy.send(Messages.PaidDebtOfIndebtedFirm.class, paymentOdDebtMessage -> {
                            paymentOdDebtMessage.debt = debt;
                        }).to(healthyFirmID);

                        // the indebted firm no longer has any debt
                        // not too sure if I need to do this in this step
                        // economy.send(Messages.NoDebt.class).to(indebtedFirmID);
                        economy.bailedOutFirmsMap.put(indebtedFirmID, new BailoutPackage(economy.healthyFirmAccountMap.get(healthyFirmID).price, economy.healthyFirmAccountMap.get(healthyFirmID).wage));
                    } else {
                        economy.deficit -= debt;
                        economy.bankruptFirmsArray.add(indebtedFirmID);
                    }
                } else {
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
