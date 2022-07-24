package macro_financial_framework.agents;

import macro_financial_framework.utils.Globals;
import macro_financial_framework.utils.HealthyFirmAccount;
import macro_financial_framework.utils.Links;
import macro_financial_framework.utils.Messages;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.*;

public class Firm extends Agent<Globals> {

    @Variable
    public int vacancies;
    @Variable
    public int workers;

    @Variable
    public int sizeOfCompany;
    @Variable
    public int sector;
    @Variable
    public double profit;
    @Variable
    public double wage;
    public double priceOfGoods;
    public double previousPrice;
    @Variable
    public double productivity;
    @Variable
    public long stock;
    public int demand;
    @Variable
    public int demandOfIntermediateGoods;
    @Variable
    public long inventory;
    public long inventoryOfIntermediateGoods;
    @Variable
    public double earnings;
    public double dividend;
    public double previousOutput;
    public double targetProduction;
    public int good;
    public double deposits = 100000;
    public int availableWorkers;
    public double averagePrice;
    public boolean isHiring;
    public int workersToBeFired;
    public int intermediateGood; // the intermediate good it needs for production of its own good, i.e. good 0
    public int stockOfIntermediateGood; // how much of that good it has
    public double intermediateGoodConstant;
    public long intermediateGoodQuantityProduced; // intermediate good produced for other firms
    public double priceOfIntermediateGood;
    public double spentOnIntermediateGoods;
    public boolean healthy;
    public boolean isProductive;
    public double debt;
    public int totalUnemployment;

    //TODO: check this
    public int productionConstant = 30;

    public static Action<Firm> SetVacancies() {
        return Action.create(Firm.class, firm -> {
            // set vacancies according to firm size
            // TODO: check if logic makes sense -> check the numbers
            // potential resources: https://www.statista.com/statistics/676671/employees-by-business-size-uk/
            // Ive scaled it down because if not there are approx 25000 vacancies
            if (firm.isProductive) {
                // TODO: Relate these values to nbFirms and nbHouseholds

                if (firm.sizeOfCompany == 0) {
                    firm.vacancies = (int) firm.getPrng().uniform(1, 10).sample();
                } else if (firm.sizeOfCompany == 1) {
                    firm.vacancies = (int) firm.getPrng().uniform(10, 100).sample();
                } else {
                    firm.vacancies = (int) firm.getPrng().uniform(100, 1000).sample();
                }
            } else {
                firm.vacancies = 0;
            }
            System.out.println("Firm " + firm.getID() + " is of size " + firm.sizeOfCompany + " and has " + firm.vacancies + " vacancies");
            //firm.getGlobals().totalVacancies += firm.vacancies;
            //System.out.println(firm.getGlobals().totalVacancies);
        });
    }

    public static Action<Firm> SetInitialStockOfIntermediateGoods() {
        return Action.create(Firm.class, firm -> {
            // I am assuming a one on one relationship, for one unit of good, one intermediate good is needed
            // Ideally, a firm will have 100% productivity so target output would be the initial vacancies * 100% productivity = initial stock of intermediate goods
            firm.stockOfIntermediateGood = firm.vacancies;
        });
    }

//    public static Action<Firm> SetSectorSpecifics() {
//        return Action.create(Firm.class, firm -> {
//            if (firm.hasMessageOfType(Messages.FirmProperties.class)) {
//                firm.getMessagesOfType(Messages.FirmProperties.class).forEach(msg -> {
//                    firm.good = msg.good;
//                    firm.intermediateGood = msg.goodToPurchase;
//                    //TODO: check if this makes sense
//                    if (firm.sizeOfCompany == 0) {
//                        firm.wage = msg.wage + firm.getPrng().uniform(-200.00, -100.00).sample(); // smaller companies pay a wage smaller than the average wage for that sector
//                    } else if (firm.sizeOfCompany == 1) {
//                        firm.wage = msg.wage + firm.getPrng().uniform(-100.00, 100.00).sample(); // medium-sized companies pay a wage that can be slightly lower or higher than average
//                    } else {
//                        firm.wage = msg.wage + firm.getPrng().uniform(100.00, 200.00).sample(); // big companies pay a wage that is higher than the average for the sector
//                    }
//                });
//            }
//            firm.deposits = firm.getGlobals().deposistsMultiplier * firm.vacancies * firm.wage;
//        });
//    }

    public static Action<Firm> SetSectorSpecificGoods() {
        return Action.create(Firm.class, firm -> {
            firm.good = firm.sector;
            firm.intermediateGood = (firm.sector + 1) % firm.getGlobals().nbGoods;
        });
    }

    public static Action<Firm> SetPriceOfGoods() {
        return Action.create(Firm.class, firm -> {
            //TODO: check how to make logical assumptions about pricing
            //TODO: price of non competitive goods should be higher
            double price = firm.getPrng().uniform(1.00, 100.00).sample();
            firm.priceOfGoods = price;
            firm.priceOfIntermediateGood = price / 2;
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

    public static Action<Firm> SetWages() {
        return Action.create(Firm.class, firm -> {
            // TODO: check this. Right now 50% of sales is the wage and the rest is profit
            double maxSales = Math.floor(firm.productivity * firm.workers) * firm.priceOfGoods * firm.productionConstant;
            double maxSalesIntGoods = Math.floor(firm.productivity * firm.workers) * (firm.priceOfIntermediateGood) * firm.productionConstant;
            double totalMaxSales = maxSales + maxSalesIntGoods;
            firm.wage = totalMaxSales / (firm.workers * 2);
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
            //TODO: check if this is a fair assumption -> I really need to check how to deal with intermediate goods
            // this means it has enough intermediate goods for production
            if (firm.isProductive) {
                if (firm.workers <= firm.stockOfIntermediateGood) {
                    firm.intermediateGoodConstant = 1.00d;
                    firm.stockOfIntermediateGood -= firm.workers;

                    // if the firm doesn't have any intermediate good it can't produce
                } else if (firm.stockOfIntermediateGood == 0) {
                    firm.intermediateGoodConstant = 0.00d;

                    // the workers a firm has is the maximum amount of goods it can produce
                    // to produce all of the goods it needs the same amount of workers as intermediate goods
                } else if (firm.stockOfIntermediateGood < firm.workers) {
                    firm.intermediateGoodConstant = firm.stockOfIntermediateGood / firm.workers;
                    firm.stockOfIntermediateGood = 0; // if it has less it will use all of it for production
                }
                firm.stock = (long) Math.floor(firm.productivity * firm.workers * firm.intermediateGoodConstant) * firm.productionConstant;

                // TODO: check this -> it produces the same amount of intermediate goods as actual goods?
                firm.intermediateGoodQuantityProduced = firm.stock;
            } else {
                firm.stock = 0;
                firm.intermediateGoodQuantityProduced = 0;
            }
            //TODO: check if this is a valid assumption
            firm.priceOfIntermediateGood = firm.priceOfGoods / 2;
        });
    }

    public static Action<Firm> SendIntermediateGoodInfo() {
        return Action.create(Firm.class, firm -> {
            // sends the message of the good it is producing as intermediate good
            firm.send(Messages.StockOfIntermediateGood.class, message -> {
                message.price = firm.priceOfIntermediateGood;
                message.stock = firm.intermediateGoodQuantityProduced;
            }).to(firm.getGlobals().goodExchangeIDs.get(firm.good));


            firm.send(Messages.PurchaseIntermediateGood.class, m -> {
                m.demand = firm.workers - firm.stockOfIntermediateGood;
            }).to(firm.getGlobals().goodExchangeIDs.get(firm.intermediateGood));

        });
    }

    // TODO: sell the inventory first
    public static Action<Firm> sendSupply() {
        return Action.create(Firm.class, firm -> {
            firm.send(Messages.FirmSupply.class, m -> {
                m.output = firm.stock;
                m.price = firm.priceOfGoods;
            }).to(firm.getGlobals().goodExchangeIDs.get(firm.good));
        });
    }

    public static Action<Firm> receiveIntermediateGoodsAndDemand() {
        return Action.create(Firm.class, firm -> {

            // receive the intermediate good needed for production
            if (firm.hasMessageOfType(Messages.IntermediateGoodBought.class)) {
                firm.getMessagesOfType(Messages.IntermediateGoodBought.class).forEach(message -> {
                    firm.stockOfIntermediateGood += message.quantity;
                    firm.spentOnIntermediateGoods = message.spent;
                });
            }

            // receive demand of the intermediate good
            if (firm.hasMessageOfType(Messages.DemandOfIntermediateGood.class)) {
                firm.demandOfIntermediateGoods = 0;
                firm.getMessagesOfType(Messages.DemandOfIntermediateGood.class).forEach(message -> {
                    firm.demandOfIntermediateGoods += message.demand;
                    firm.intermediateGoodQuantityProduced -= message.bought;
                    firm.earnings += message.bought * firm.priceOfIntermediateGood;
                });

                firm.inventoryOfIntermediateGoods += firm.intermediateGoodQuantityProduced;
                firm.intermediateGoodQuantityProduced = 0;
            }
        });
    }

    public static Action<Firm> receiveDemand() {
        return Action.create(Firm.class, firm -> {

            // the initial stock of the product of that firm
            // I am storing how much the firm produced, as when the selling occurs the stock will go down
            // needed for various metrics and calculations
            firm.previousOutput = firm.stock;
            firm.demand = 0;

            if (firm.hasMessageOfType(Messages.HouseholdWantsToPurchase.class)) {
                firm.getMessagesOfType(Messages.HouseholdWantsToPurchase.class).forEach(purchaseMessage -> {
                    firm.demand += purchaseMessage.demand;
                    firm.stock -= purchaseMessage.bought;
                    // cash received from the goods sold -> without subtracting costs of production and payment to workers/investors
                    firm.earnings += firm.priceOfGoods * purchaseMessage.bought;

                });

            }
            firm.inventory += firm.stock;
            firm.stock = 0;
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
                firm.profit = firm.earnings - (firm.wage * firm.workers);
                firm.deposits += firm.profit;

                // check if the firm can pay dividends
                if (firm.profit > 0 && firm.deposits > 0) {
                    firm.dividend = firm.profit * firm.getGlobals().delta;
                    firm.profit -= firm.dividend;
                    firm.getLinks(Links.FirmToInvestorLink.class).send(Messages.PayInvestors.class, (dividendPayment, firmToInvestorLink) -> {
                        dividendPayment.dividend = firm.dividend;
                    });
                }

                firm.earnings = 0;

                // check health of the firm
                if (firm.deposits > firm.getGlobals().Theta * firm.previousOutput * firm.wage) {
                    firm.healthy = true;
                }
            }
        });
    }

    public static Action<Firm> sendBailoutRequest() {
        return Action.create(Firm.class, firm -> {
            if (firm.isProductive & (firm.deposits < -firm.getGlobals().Theta * firm.previousOutput * firm.wage))
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
            });
        });
    }

    public static Action<Firm> doRevival() {
        return Action.create(Firm.class, firm -> {
            if (!firm.isProductive & (firm.getPrng().uniform(0, 1).sample() < firm.getGlobals().phi)) {
                firm.isProductive = true;
                firm.priceOfGoods = firm.averagePrice;
                firm.targetProduction = firm.getGlobals().mu * firm.totalUnemployment;
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
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmsPrice.class, (m, l) -> {
                    m.price = firm.priceOfGoods;
                    m.output = firm.stock;
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
                firm.targetProduction = firm.previousOutput + Math.min(firm.getGlobals().etta_plus * (firm.demand - firm.previousOutput), firm.getGlobals().mu * firm.availableWorkers);
                if (firm.priceOfGoods < firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d + firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            } else if (firm.previousOutput < firm.demand) {
                if (firm.profit > 0) {
                    firm.wage = Math.min(firm.wage, (firm.priceOfGoods * Math.min(firm.demand, firm.previousOutput)) / firm.previousOutput);
                    firm.wage = firm.wage * (1.0d + firm.getGlobals().gamma_w * firm.availableWorkers * firm.getPrng().uniform(0, 1).sample());
                }
                // TODO: target production needs to be looked at - prices of goods are very low compared to salaries
                firm.targetProduction = Math.max(0.0d, firm.previousOutput - (firm.getGlobals().etta_minus * (firm.previousOutput - firm.demand)));
                if (firm.priceOfGoods > firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d - firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            }
            firm.demand = 0;
        });
    }

    public static Action<Firm> UpdateVacancies() {
        return Action.create(Firm.class, firm -> {
            // all are set to true before the conditions are checked
            firm.isHiring = true;
            int targetWorkers = (int) Math.ceil(firm.targetProduction/ firm.productionConstant); //assuming a one to one relationship
            System.out.println("Firm " + firm.getID() + " target workers: " + targetWorkers + " current workers " + firm.workers);
            if (targetWorkers > firm.workers) {
                firm.vacancies = targetWorkers - firm.workers;
                System.out.println("condition 1");
            } else if (targetWorkers == firm.workers) {
                firm.vacancies = 0;
                firm.isHiring = false;
                firm.workersToBeFired = 0;
                System.out.println("condition 2");
            } else if (targetWorkers < firm.workers) {
                firm.isHiring = false;
                firm.vacancies = 0;
                firm.workersToBeFired = Math.abs(firm.workers - targetWorkers);
                System.out.println("condition 3" + " workers to be fired " + firm.workersToBeFired);
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
                int firedWorkers = 0;
                if (workers.size() < firm.workersToBeFired) {
                    System.out.println("firm " + firm.getID() + " has " + workers.size() + " workers and needs to fire " + firm.workersToBeFired);
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
//                }
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

    public void determineSizeOfCompany() {
        double rand = getPrng().uniform(0, 1).sample();
        if (rand < getGlobals().percentMicroSmallFirms) {
            sizeOfCompany = 0;
        } else {
            sizeOfCompany = 1;
        }
    }
}

