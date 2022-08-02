package SimpleEconomyModel.agents;

import SimpleEconomyModel.utils.Globals;
import SimpleEconomyModel.utils.HealthyFirmAccount;
import SimpleEconomyModel.utils.Links;
import SimpleEconomyModel.utils.Messages;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.*;
import java.util.stream.Collectors;

public class Firm extends Agent<Globals> {

    @Variable
    public int vacancies;
    @Variable
    public double deposits;
    @Variable
    public double profit;
    @Variable
    public double wage;
    public long stock;
    public long inventory;
    @Variable
    public double earnings;
    public int workers;
    public int sizeOfCompany;
    public int sector;
    public double priceOfGoods;
    public double productivity;
    public int demand;
    public double dividend;
    public double previousOutput;
    public double targetProduction;
    public int good;
    public int availableWorkers;
    public double averagePrice;
    public boolean isHiring;
    public int workersToBeFired;
    public double intermediateGoodConstant;
    public double spentOnIntermediateGoods;
    public boolean healthy;
    public boolean isProductive;
    public double totalUnemployment;
    public HashMap<Integer, Double> stockOfIntermediateGood;
    public HashMap<Integer, Double> IntermediateGoodNeeded;
    public double totalStockOfIntGood;
    public boolean needsIntermediateGoods = false;

    public static Action<Firm> SetVacancies() {
        return Action.create(Firm.class, firm -> {
            // set vacancies according to firm size
            // TODO: check if logic makes sense -> check the numbers
            // potential resources: https://www.statista.com/statistics/676671/employees-by-business-size-uk/
            // Ive scaled it down because if not there are approx 25000 vacancies
            if (firm.isProductive) {
                if (firm.sizeOfCompany == 0) {
                    firm.vacancies = (int) firm.getPrng().uniform(1, 5).sample();
                } else if (firm.sizeOfCompany == 1) {
                    firm.vacancies = (int) firm.getPrng().uniform(5, 50).sample();
                } else {
                    firm.vacancies = (int) firm.getPrng().uniform(50, 100).sample();
                }
            } else {
                firm.vacancies = 0;
            }
            firm.getGlobals().totalVacancies += firm.vacancies;
//            System.out.println("Firm " + firm.getID() + " is of size " + firm.sizeOfCompany + " and has " + firm.vacancies + " vacancies ");
//            System.out.println("Total vacancies in the economy " + firm.getGlobals().totalVacancies);
        });
    }

    public static Action<Firm> SetWages() {
        return Action.create(Firm.class, firm -> {
            // TODO: check this. Right now 50% of sales is the wage and the rest is profit
            double maxSales = firm.vacancies * firm.priceOfGoods * firm.getGlobals().productionConstant;
            firm.wage = maxSales / (firm.vacancies * 2);
        });
    }

    public static Action<Firm> SetInitialStockOfIntermediateGoods() {
        return Action.create(Firm.class, firm -> {

            // enough intermediate goods for two sets of production based on the vacancies of the firm
            int predOutput = firm.vacancies * 2 * firm.getGlobals().productionConstant;
            for (int i = 0; i < firm.getGlobals().nbGoods; i++) {
//                 // for debugging purposes
//                System.out.println(firm.getGlobals().weightsArray[firm.sector][i]);
                firm.stockOfIntermediateGood.put(i, firm.getGlobals().weightsArray[firm.sector][i] * predOutput);
            }
        });
    }

    public static Action<Firm> SetSectorSpecificGoods() {
        return Action.create(Firm.class, firm -> {
            firm.good = firm.sector;
            firm.deposits = firm.getGlobals().deposistsMultiplier * firm.vacancies * firm.wage;
        });
    }

    public static Action<Firm> SetPriceOfGoods() {
        return Action.create(Firm.class, firm -> {
            if (firm.sector == 0) {
                firm.priceOfGoods = 270;
            } else if (firm.sector == 1) {
                firm.priceOfGoods = 75;
            } else if (firm.sector == 2) {
                firm.priceOfGoods = firm.getPrng().uniform(85.00, 110.00).sample();
            } else if (firm.sector == 3) {
                double rand = firm.getPrng().uniform(0, 1).sample();
                if (rand < 0.95) {
                    firm.priceOfGoods = firm.getPrng().uniform(1.00, 10.00).sample();
                } else {
                    firm.priceOfGoods = firm.getPrng().uniform(10.00, 1000.00).sample();
                }
            } else if (firm.sector == 4) {
                firm.priceOfGoods = 122;
            } else if (firm.sector == 5) {
                firm.priceOfGoods = 19;
            } else if (firm.sector == 6) {
                firm.priceOfGoods = 3900.;
            } else if (firm.sector == 7) {
                firm.priceOfGoods = firm.getPrng().uniform(350, 2000).sample();
            } else if (firm.sector == 8) {
                firm.priceOfGoods = firm.getPrng().uniform(700, 800).sample();
            } else {
                firm.priceOfGoods = 30;
            }
            firm.priceOfGoods /= 10;
        });
    }


    public static Action<Firm> FindInvestors() {
        // send info to the economy agent to find an investor
        return Action.create(Firm.class, firm -> {
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FindInvestor.class);
        });
    }

    public static Action<Firm> AssignFirmInvestor() {
        // each firm is assigned one investor
        return Action.create(Firm.class, firm -> {
            if (firm.hasMessageOfType(Messages.InvestorOfFirm.class)) {
                firm.getMessagesOfType(Messages.InvestorOfFirm.class).forEach(msg -> {
                    firm.addLink(msg.investorID, Links.FirmToInvestorLink.class);
                });
            }
        });
    }

    public static Action<Firm> sendInfo() {
        // send vacancies of the firm
        return Action.create(Firm.class, firm -> {
            // al firms are hiring at the beggining
            if (firm.isHiring && firm.isProductive) {
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                    msg.vacancies = firm.vacancies;
                    msg.sector = firm.sector;
                });
            }
        });
    }

    public static Action<Firm> updateVacancies() {
        return Action.create(Firm.class, firm -> {
            // hires workers and adjusts vacancies
            if (firm.hasMessageOfType(Messages.NewEmployee.class)) {
                firm.getMessagesOfType(Messages.NewEmployee.class).forEach(msg -> {
                    firm.addLink(msg.workerID, Links.FirmToWorkerLink.class);
                    firm.vacancies--;
                    firm.workers++;
                });
            }
        });
    }


    public static Action<Firm> CalculateFirmProductivity() {
        return Action.create(Firm.class, firm -> {

            firm.productivity = 0;
            // productivity is set to 0 by default
            if (firm.hasMessageOfType(Messages.Productivity.class)) {
                firm.getMessagesOfType(Messages.Productivity.class).forEach(msg -> {
                    firm.productivity += msg.productivity;
                });
                firm.productivity /= firm.workers;
            }
        });
    }


    public static Action<Firm> FirmsProduce() {
        return Action.create(Firm.class, firm -> {
            // depending on the size of the firm -> workers can produce more or less goods
            // this is assuming that larger companies have more facilities
            if (firm.isProductive) {
                firm.stockOfIntermediateGood.forEach((good, quantity) -> {
                    firm.totalStockOfIntGood += quantity;
                });

                if (firm.totalStockOfIntGood < 1.00d) {
                    firm.intermediateGoodConstant = 0;
                } else {
                    firm.intermediateGoodConstant = 1;
                }

                firm.stock = (long) Math.ceil(firm.productivity * firm.workers * firm.intermediateGoodConstant) * firm.getGlobals().productionConstant;

                List<Integer> ProductionFromIntGoods = new ArrayList<Integer>();
                firm.stockOfIntermediateGood.forEach((good, quantity) -> {
                    ProductionFromIntGoods.add((int) Math.floor(quantity / firm.getGlobals().weightsArray[firm.sector][good]));
                });

                int minProduction = ProductionFromIntGoods.stream().sorted().collect(Collectors.toList()).stream().findFirst().get() * firm.getGlobals().productionConstant;

                if (firm.stock > 0) {
                    if (minProduction < firm.stock) {
                        // it can't produce what it intended it because it doesn't have enough intermediate goods
                        // in this condition it means that its used all of the intermediate goods for production
                        firm.stock = minProduction;
                        firm.stockOfIntermediateGood.clear();
                        for (int good = 0; good < firm.getGlobals().nbGoods; good++) {
                            firm.stockOfIntermediateGood.put(good, 0.00d);
                        }
                    } else if (firm.stock < minProduction) {
                        // update consumption of intermediate goods from what is used in production
                        for (int good = 0; good < firm.getGlobals().nbGoods; good++) {
                            double initialQuantity = firm.stockOfIntermediateGood.get(good);
                            firm.stockOfIntermediateGood.remove(good);
                            double quantity = initialQuantity - (firm.stock * firm.getGlobals().weightsArray[firm.sector][good]);
                            firm.stockOfIntermediateGood.put(good, quantity);
                        }
                    }
                }
            } else {
                firm.stock = 0;
            }
//            System.out.println("firm " + firm.getID() + " has produced " + firm.stock + " and has " + firm.workers + " workers and productivity " + firm.productivity);
        });
    }

    // TODO: sell the inventory first
    public static Action<Firm> sendSupplyAndDemand() {
        return Action.create(Firm.class, firm -> {
            // send information about the stock of the good it produces and the price of the good
            firm.send(Messages.FirmSupply.class, m -> {
//                System.out.println("firm " + firm.getID() + " is sending " + firm.stock + " to good market " + firm.getGlobals().goodExchangeIDs.get(firm.good));
//                System.out.println("firm " + firm.getID() + " has " + firm.workers + " workers");
                m.output = firm.stock;
                m.price = firm.priceOfGoods;
            }).to(firm.getGlobals().goodExchangeIDs.get(firm.good));

            firm.inventory = 0;

            // calculate how much intermediate good it needs for target production
            // sends the demand of intermediate good
            if (firm.needsIntermediateGoods) {
                firm.IntermediateGoodNeeded.forEach((good, quantity) -> {
                    firm.send(Messages.PurchaseIntermediateGood.class, m -> {
                        m.demand = (int) Math.ceil(quantity);
                    }).to(firm.getGlobals().goodExchangeIDs.get(good));
                });
                firm.IntermediateGoodNeeded.clear();
            }
        });
    }

    public static Action<Firm> receiveDemandAndIntermediateGoods() {
        return Action.create(Firm.class, firm -> {

            // the initial stock of the product of that firm
            // I am storing how much the firm produced, as when the selling occurs the stock will go down
            // needed for various metrics and calculations
            firm.previousOutput = firm.stock;
            firm.demand = 0;
//            System.out.println("firm " + firm.getID() + " workers: " + firm.workers);
            if (firm.hasMessageOfType(Messages.HouseholdOrFirmWantsToPurchase.class)) {
                firm.getMessagesOfType(Messages.HouseholdOrFirmWantsToPurchase.class).forEach(purchaseMessage -> {
//                    System.out.println("Firm " + firm.getID() + " has sold " + purchaseMessage.bought + " to household " + purchaseMessage.getSender()
//                    + " and previously produced " + firm.stock + " demand " + purchaseMessage.demand);
                    firm.demand += purchaseMessage.demand;
                    firm.stock -= purchaseMessage.bought;
                    // cash received from the goods sold -> without subtracting costs of production and payment to workers/investors
                    firm.earnings += firm.priceOfGoods * purchaseMessage.bought;
//                    System.out.println("Firm " + firm.getID() + " demand: " + purchaseMessage.demand + " stock: " + firm.previousOutput + " bought: " + purchaseMessage.bought);
                });
//                System.out.println("Firm " + firm.getID() + " has earnings of: " + firm.earnings + " the firm has price of good of " + firm.priceOfGoods + " and wage of " + firm.wage);
            }
//            System.out.println("Firm " + firm.getID() + " has a total demand of " + firm.demand + " and had previously produced " + firm.previousOutput);
            firm.inventory += firm.stock;
            firm.stock = 0;

            if (firm.hasMessageOfType(Messages.IntermediateGoodBought.class)) {
                firm.getMessagesOfType(Messages.IntermediateGoodBought.class).forEach(intGoodMessage -> {
                    double prevQuantity = firm.stockOfIntermediateGood.get(intGoodMessage.good);
                    firm.stockOfIntermediateGood.remove(intGoodMessage.good);
                    firm.stockOfIntermediateGood.put(intGoodMessage.good, prevQuantity + intGoodMessage.quantity);
                    firm.spentOnIntermediateGoods += intGoodMessage.spent;
                });
            }
        });
    }


    public static Action<Firm> payWorkers() {
        // pays the workers
        return Action.create(Firm.class, firm -> {
            firm.getLinks(Links.FirmToWorkerLink.class).send(Messages.WorkerPayment.class, (msg, link) -> {
                msg.wage = firm.wage;
            });
        });
    }

    public static Action<Firm> GetAveragePrice() {
        return Action.create(Firm.class, firm -> {
            firm.getMessagesOfType(Messages.AveragePrice.class).forEach(msg -> {
                firm.averagePrice = msg.averagePrice;
            });
        });
    }

    public static Action<Firm> Accounting() {
        return Action.create(Firm.class, firm -> {
            // the profit is the difference between the cost of production (wage * nb of employees) and the products sold
            // might be negative as even if firms don't sell anything, they need to pay workers
            firm.healthy = false;
            if (firm.isProductive) {
                firm.profit = firm.earnings - (firm.wage * firm.workers) - firm.spentOnIntermediateGoods;
//                System.out.println("firm " + firm.getID() + " has profit of: " + firm.profit + " workers: " + firm.workers);

                // check if the firm can pay dividends
                if (firm.profit > 0 && firm.deposits > 0) {
                    firm.dividend = firm.profit * firm.getGlobals().delta;
                    firm.profit -= firm.dividend;
                    firm.getLinks(Links.FirmToInvestorLink.class).send(Messages.PayInvestors.class, (dividendPayment, firmToInvestorLink) -> {
                        dividendPayment.dividend = firm.dividend;
                    });
                }
                if (firm.profit < 0){
//                    System.out.println("firm " + firm.getID() + " has negative profit and has " + firm.workers + " workers");
                }
                firm.deposits += firm.profit;
                firm.earnings = 0;
                firm.spentOnIntermediateGoods = 0;

                // check health of the firm
                if (firm.deposits > firm.getGlobals().Theta * firm.workers * firm.wage) {
                    firm.healthy = true;
                }
            }
        });
    }

    public static Action<Firm> sendBailoutRequest() {
        return Action.create(Firm.class, firm -> {
            if (firm.isProductive & (firm.deposits < -firm.getGlobals().Theta * firm.workers * firm.wage))
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.BailoutRequestMessage.class, (msg, link) -> {
                    msg.debt = firm.deposits;
                });
        });
    }

    public static Action<Firm> sendHealthyFirmAccount() {
        return Action.create(Firm.class, firm -> {
            if (firm.healthy)
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.HealthyFirmAccountMessage.class, (msg, link) -> {
                    msg.healthyFirmAccount = new HealthyFirmAccount(firm.deposits, firm.priceOfGoods, firm.wage);
                });
        });
    }

    public static Action<Firm> paymentOfIndebtedFirm() {
        return Action.create(Firm.class, firm -> {
            if (firm.hasMessageOfType(Messages.PaidDebtOfIndebtedFirm.class)) {
                firm.getMessagesOfType(Messages.PaidDebtOfIndebtedFirm.class).forEach(msg -> {
                    firm.deposits += msg.debt;
                });
            }
        });
    }

    public static Action<Firm> receiveBailoutPackage() {
        return Action.create(Firm.class, firm -> {
            firm.getMessagesOfType(Messages.BailoutPackageMessage.class).forEach(msg -> {
                firm.priceOfGoods = msg.price;
                firm.wage = msg.wage;
                firm.deposits = 0;
            });
        });
    }

    public static Action<Firm> receiveBankruptcyMessage() {
        return Action.create(Firm.class, firm -> {
            firm.getMessagesOfType(Messages.BankruptcyMessage.class).forEach(msg -> {
                firm.deposits = 0;
                firm.isProductive = false;
                firm.targetProduction = 0;
//                System.out.println("Firm " + firm.getID() + " is bankrupt");
            });
        });
    }

    public static Action<Firm> doRevival() {
        return Action.create(Firm.class, firm -> {
            if (!firm.isProductive & (firm.getPrng().uniform(0, 1).sample() < firm.getGlobals().phi)) {
                firm.isProductive = true;
                firm.priceOfGoods = firm.averagePrice;
                firm.targetProduction = firm.getGlobals().mu * (firm.totalUnemployment * firm.getGlobals().nbWorkers);
                firm.deposits = firm.targetProduction * firm.wage;

                // send message to investor so that he revives the firm
                firm.getLinks(Links.FirmToInvestorLink.class).send(Messages.InvestorPaysRevival.class, (m, l) -> {
                    m.debt = firm.deposits;
                });
            }
        });
    }


    public static Action<Firm> sendInfoToGetAvailableWorkers() {
        return Action.create(Firm.class, firm -> {
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmGetAvailableWorkers.class, (m, l) -> {
                m.sector = firm.sector;
            });
        });
    }

    public static Action<Firm> receiveAvailableWorkers() {
        return Action.create(Firm.class, firm -> {
            if (firm.hasMessageOfType(Messages.AvailableWorkersInYourSector.class)) {
                firm.getMessagesOfType(Messages.AvailableWorkersInYourSector.class).forEach(msg -> {
                    firm.availableWorkers = msg.workers;
                });
            }
            if (firm.hasMessageOfType(Messages.CurrentUnemployment.class)) {
                firm.getMessagesOfType(Messages.CurrentUnemployment.class).forEach(msg -> {
                    firm.totalUnemployment = msg.unemployment;
                });
            }
        });
    }

    public static Action<Firm> sendPrice() {
        return Action.create(Firm.class, firm -> {
            // at this point I havent set the stock to 0
            if (firm.stock > 0) {
                //System.out.println("Firm " + firm.getID() + " has produced " + firm.stock + " units of good at a price of " + firm.priceOfGoods);
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmsPrice.class, (m, l) -> {
                    m.price = firm.priceOfGoods;
                    m.output = firm.stock;
                });
            }
            if (firm.demand > 0) {
                //System.out.println("Firm " + firm.getID() + " has produced " + firm.stock + " units of good at a price of " + firm.priceOfGoods);
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmsPriceDemandedGoods.class, (m, l) -> {
                    m.price = firm.priceOfGoods;
                    m.output = firm.demand;
                });
            }
        });
    }

    public static Action<Firm> adjustPriceProduction() {
        return Action.create(Firm.class, firm -> {
            if (firm.previousOutput > firm.demand) {
                if (firm.profit < 0) {
                    firm.wage = firm.wage * (1.0d - firm.getGlobals().gamma_w * firm.availableWorkers * firm.getPrng().uniform(0, 1).sample());
                }
                firm.targetProduction = Math.ceil(firm.previousOutput + Math.min(firm.getGlobals().etta_plus * (firm.demand - firm.previousOutput), firm.getGlobals().mu * firm.availableWorkers));

                if (firm.priceOfGoods < firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d + firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            } else if (firm.previousOutput < firm.demand) {
                if (firm.profit > 0) {
                    firm.wage = Math.min(firm.wage, (firm.priceOfGoods * Math.min(firm.demand, firm.previousOutput)) / firm.previousOutput);
                    firm.wage = firm.wage * (1.0d + firm.getGlobals().gamma_w * firm.availableWorkers * firm.getPrng().uniform(0, 1).sample());
                }
                firm.targetProduction = Math.ceil(Math.max(0.0d, firm.previousOutput - (firm.getGlobals().etta_minus * (firm.previousOutput - firm.demand))));
                if (firm.priceOfGoods > firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d - firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            }
//            System.out.println("firm " + firm.getID() + " target production: " + firm.targetProduction);

            // below we are accounting for what the firm already has in its inventory
//            if (firm.inventory >= firm.targetProduction) {
//                firm.targetProduction = 0;
//            } else {
//                firm.targetProduction -= firm.inventory;
//            }
//            // for debugging purposes
//            System.out.println("target production " + firm.targetProduction);
            firm.demand = 0;
        });
    }

    public static Action<Firm> CheckIfIntGoodsNeeded() {
        return Action.create(Firm.class, firm -> {

            // this is the target production scaled down
            int production = (int) Math.ceil(firm.targetProduction);

            // iterate over the current stock of intermediate good to check if it can meet its target production
            // if not, purchase intermediate good
            firm.stockOfIntermediateGood.forEach((good, quantity) -> {
                int maxProduction = (int) Math.floor(quantity / firm.getGlobals().weightsArray[firm.sector][good]);

                // if there isn't enough intermediate good for the target production, purchase more
                if (maxProduction < production) {
                    firm.needsIntermediateGoods = true;
                    firm.IntermediateGoodNeeded.put(good, (production - maxProduction) * firm.getGlobals().weightsArray[firm.sector][good]);
                }
            });
        });
    }

    public static Action<Firm> UpdateVacancies() {
        return Action.create(Firm.class, firm -> {
            // all are set to true before the conditions are checked
            firm.isHiring = true;
            int targetWorkers = (int) ((int) (Math.ceil(firm.targetProduction / firm.getGlobals().productionConstant)) / firm.productivity); //assuming a one to one relationship
//            System.out.println("Firm " + firm.getID() + " target workers: " + targetWorkers + " current workers " + firm.workers);
            if (targetWorkers > firm.workers) {
                firm.vacancies = targetWorkers - firm.workers;
//                System.out.println("condition 1");
            } else if (targetWorkers == firm.workers) {
                firm.vacancies = 0;
                firm.isHiring = false;
                firm.workersToBeFired = 0;
//                System.out.println("condition 2");
            } else if (targetWorkers < firm.workers) {
                firm.isHiring = false;
                firm.vacancies = 0;
                firm.workersToBeFired = Math.abs(firm.workers - targetWorkers);
//                System.out.println("condition 3" + " workers to be fired " + firm.workersToBeFired);
            } else if (targetWorkers == 0){
                firm.vacancies = 1;
            }
        });
    }

    public static Action<Firm> FireWorkers() {
        return Action.create(Firm.class, firm -> {

            // a treemap sorts the key values in ascending order, i.e. from lower to higher productivity
            TreeMap<Double, Long> workers = new TreeMap<>();
            if (firm.hasMessageOfType(Messages.JobCheck.class)) {
                firm.getMessagesOfType(Messages.JobCheck.class).forEach(msg -> {
                    workers.put(msg.productivity, msg.getSender());
                });
            }

            if (!firm.isHiring && firm.workersToBeFired > 0) {
//                System.out.println("firm " + firm.getID() + " has " + workers.size() + " workers and needs to fire " + firm.workersToBeFired
//                        + " and the target production is " + firm.targetProduction);
                int firedWorkers = 0;
                if (workers.size() < firm.workersToBeFired) {
                    firm.workersToBeFired = workers.size();
//                    System.out.println("firm " + firm.getID() + " has " + workers.size() + " workers and needs to fire " + firm.workersToBeFired
//                            + " and the target production is " + firm.targetProduction);
                }
                // TODO: Check why so many workers are being fired
                while (firedWorkers < firm.workersToBeFired) {
                    double workerKey = (double) workers.keySet().toArray()[firedWorkers];
                    long workerID = workers.get(workerKey);
                    firm.send(Messages.Fired.class).to(workerID);
                    firm.removeLinksTo(workerID);
                    firedWorkers++;
                    firm.workers--;
                }
            }
        });
    }

    public static Action<Firm> UpdateFirmSize() {
        return Action.create(Firm.class, firm -> {
            if (firm.workers <= 10) {
                firm.sizeOfCompany = 0;
            } else if (firm.workers > 10 && firm.workers <= 100) {
                firm.sizeOfCompany = 1;
            } else {
                firm.sizeOfCompany = 2;
            }
        });
    }

    public static Action<Firm> checkProductiveFirms() {
        return Action.create(Firm.class, firm -> {
            if (!firm.isProductive) {
//                System.out.println("Firm " + firm.getID() + " is not productive");
            }
        });
    }

    public void determineSizeOfCompany() {
        double rand = getPrng().uniform(0, 1).sample();
        if (rand < getGlobals().percentMicroFirms) {
            sizeOfCompany = 0;
        } else if (rand >= getGlobals().percentMicroFirms && rand < (getGlobals().percentMicroFirms + getGlobals().percentSmallFirms)) {
            sizeOfCompany = 1;
        } else {
            sizeOfCompany = 2;
        }
    }
}

