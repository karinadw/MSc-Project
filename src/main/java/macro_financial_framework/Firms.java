package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;

import java.util.*;

public class Firms extends Agent<MacroFinancialModel.Globals> {

    @Variable
    public int vacancies;
    @Variable
    public int workers;
    public int sizeOfCompany;
    @Variable
    public int sector;
    @Variable
    public double profit = 0;
    @Variable
    public double wage;
    @Input
    public double firingRate;
    public double priceOfGoods;
    @Variable
    public double productivity = 0.00d;
    @Variable
    public long stock;
    public int demand;
    @Variable
    public int demandOfIntermediateGoods = 0;
    @Variable
    public long inventory = 0;
    public long inventoryOfIntermediateGoods = 0;
    @Variable
    public double earnings = 0.0d;
    public double dividend = 0;
    public double previousOutput;
    public double targetProduction;
    public int good;
    public double deposits = 100000000;
    public int availableWorkers;
    public double averagePrice;
    public boolean isHiring = true;
    public int workersToBeFired = 0;
    public int intermediateGood; // the intermediate good it needs for production of its own good, i.e. good 0
    public int stockOfIntermediateGood; // how much of that good it has
    public double intermediateGoodConstant;
    public long intermediateGoodQuantityProduced; // intermediate good produced for other firms
    public double priceOfIntermediateGood;
    public double spentOnIntermediateGoods;
    public boolean healthy;
    public boolean isProductive = true;

    public static Action<Firms> SetVacancies() {
        return Action.create(Firms.class, firm -> {
            // set vacancies according to firm size
            // TODO: check if logic makes sense -> check the numbers
            // potential resources: https://www.statista.com/statistics/676671/employees-by-business-size-uk/
            if (firm.isProductive) {
                if (firm.sizeOfCompany == 0) {
                    firm.vacancies = (int) firm.getPrng().uniform(1, 50).sample();
                } else if (firm.sizeOfCompany == 1) {
                    firm.vacancies = (int) firm.getPrng().uniform(50, 1000).sample();
                } else {
                    firm.vacancies = (int) firm.getPrng().uniform(1000, 5000).sample();
                }
            } else {
                firm.vacancies = 0;
            }
        });
    }

    public static Action<Firms> SetInitialStockOfIntermediateGoods() {
        return Action.create(Firms.class, firm -> {
            // I am assuming a one on one relationship, for one unit of good, one intermediate good is needed
            // Ideally, a firm will have 100% productivity so target output would be the initial vacancies * 100% productivity = initial stock of intermediate goods
            firm.stockOfIntermediateGood = firm.vacancies;
        });
    }

    public static Action<Firms> SetSectorSpecifics() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.FirmProperties.class)) {
                firm.getMessagesOfType(Messages.FirmProperties.class).forEach(msg -> {
                    firm.good = msg.good;
                    firm.intermediateGood = msg.goodToPurchase;
                    //TODO: check if this makes sense
                    if (firm.sizeOfCompany == 0) {
                        firm.wage = msg.wage + firm.getPrng().uniform(-200.00, -100.00).sample(); // smaller companies pay a wage smaller than the average wage for that sector
                    } else if (firm.sizeOfCompany == 1) {
                        firm.wage = msg.wage + firm.getPrng().uniform(-100.00, 100.00).sample(); // medium-sized companies pay a wage that can be slightly lower or higher than average
                    } else {
                        firm.wage = msg.wage + firm.getPrng().uniform(100.00, 200.00).sample(); // big companies pay a wage that is higher than the average for the sector
                    }
                });
            }
        });
    }

    public static Action<Firms> SetPriceOfGoods() {
        return Action.create(Firms.class, firm -> {
            //TODO: check how to make logical assumptions about pricing
            //TODO: add the possibility of having another good
            double price = firm.getPrng().uniform(10.00, 1000.00).sample();
            firm.priceOfGoods = price;
        });
    }


    public static Action<Firms> FindInvestors() {
        // send info to the economy agent to find an investor
        return Action.create(Firms.class, firm -> {
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FindInvestor.class);
        });
    }

    public static Action<Firms> AssignFirmInvestor() {
        // each firm is assigned one investor
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.InvestorOfFirm.class)) {
                firm.getMessagesOfType(Messages.InvestorOfFirm.class).forEach(msg -> {
                    firm.addLink(msg.investorID, Links.FirmToInvestorLink.class);
                });
            }
        });
    }

    public static Action<Firms> sendVacancies() {
        // send vacancies of the firm
        return Action.create(Firms.class, firms -> {
            // al firms are hiring at the beggining
            if (firms.isHiring && firms.isProductive) {
                firms.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                    msg.vacancies = firms.vacancies;
                    msg.sector = firms.sector;
                });
            }
        });
    }

    public static Action<Firms> updateVacancies() {
        return Action.create(Firms.class, firm -> {
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


    public static Action<Firms> CalculateFirmProductivity() {
        return Action.create(Firms.class, firm -> {

            // productivity is set to 0 by default
            if (firm.hasMessageOfType(Messages.Productivity.class)) {
                firm.getMessagesOfType(Messages.Productivity.class).forEach(msg -> {
                    firm.productivity += msg.productivity;
                });
                firm.productivity /= firm.workers;
            }
        });
    }


    public static Action<Firms> FirmsProduce() {
        return Action.create(Firms.class, firm -> {
            // depending on the size of the firm -> workers can produce more or less goods
            // this is assuming that larger companies have more facilities
            //TODO: check if this is a fair assumption
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
                firm.stock = (long) Math.floor(firm.productivity * firm.workers * firm.intermediateGoodConstant);
                firm.intermediateGoodQuantityProduced = firm.stock;
            } else {
                firm.stock = 0;
                firm.intermediateGoodQuantityProduced = 0;
            }
            //TODO: check if this is a valid assumption
            firm.priceOfIntermediateGood = firm.priceOfGoods / 2;
        });
    }

    public static Action<Firms> SendIntermediateGoodInfo() {
        return Action.create(Firms.class, firm -> {
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
    public static Action<Firms> sendSupply() {
        return Action.create(Firms.class, firm -> {
            firm.send(Messages.FirmSupply.class, m -> {
                m.output = firm.stock;
                m.price = firm.priceOfGoods;
            }).to(firm.getGlobals().goodExchangeIDs.get(firm.good));
        });
    }

    public static Action<Firms> receiveIntermediateGoodsAndDemand() {
        return Action.create(Firms.class, firm -> {

            // receive the intermediate good needed for production
            if (firm.hasMessageOfType(Messages.IntermediateGoodBought.class)) {
                firm.getMessagesOfType(Messages.IntermediateGoodBought.class).forEach(message -> {
                    firm.stockOfIntermediateGood += message.quantity;
                    firm.spentOnIntermediateGoods = message.spent;
                });
            }

            // receive demand of the intermediate good
            if (firm.hasMessageOfType(Messages.DemandOfIntermediateGood.class)) {
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

    public static Action<Firms> receiveDemand() {
        return Action.create(Firms.class, firm -> {

            // the initial stock of the product of that firm
            // I am storing how much the firm produced, as when the selling occurs the stock will go down
            // needed for various metrics and calculations
            firm.previousOutput = firm.stock;

            if (firm.hasMessageOfType(Messages.HouseholdWantsToPurchase.class)) {
                firm.getMessagesOfType(Messages.HouseholdWantsToPurchase.class).forEach(purchaseMessage -> {
                    firm.demand += purchaseMessage.demand;
                    if (firm.stock > 0) {
                        firm.stock -= purchaseMessage.bought;
                        // cash received from the goods sold -> without subtracting costs of production and payment to workers/investors
                        firm.earnings += firm.priceOfGoods * purchaseMessage.bought;
                    }
                });

            }
            firm.inventory += firm.stock;
            firm.stock = 0;
        });
    }


    public static Action<Firms> payWorkers() {
        // pays the workers
        return Action.create(Firms.class, firm -> {
            firm.getLinks(Links.FirmToWorkerLink.class).send(Messages.WorkerPayment.class, (msg, link) -> {
                msg.wage = firm.wage;
            });
        });
    }

    public static Action<Firms> GetAveragePrice() {
        return Action.create(Firms.class, firm -> {
            firm.getMessagesOfType(Messages.AveragePrice.class).forEach(msg -> {
                firm.averagePrice = msg.averagePrice;
            });
        });
    }

    public static Action<Firms> Accounting() {
        return Action.create(Firms.class, firm -> {
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

                // check health of the firm
                if (firm.deposits > firm.getGlobals().Theta * firm.previousOutput * firm.wage) {
                    firm.healthy = true;
                }
            }
        });
    }

    public static Action<Firms> sendBailoutRequest() {
        return Action.create(Firms.class, firm -> {
            if (firm.isProductive & (firm.deposits < -firm.getGlobals().Theta * firm.previousOutput * firm.wage))
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.BailoutRequestMessage.class, (msg, link) -> {
                    msg.debt = firm.deposits;
                });
        });
    }

    public static Action<Firms> sendHealthyFirmAccount() {
        return Action.create(Firms.class, firm -> {
            if (firm.healthy)
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.HealthyFirmAccountMessage.class, (msg, link) -> {
                    msg.healthyFirmAccount = new HealthyFirmAccount(firm.deposits, firm.priceOfGoods, firm.wage);
                });
        });
    }

    public static Action<Firms> paymentOfIndebtedFirm() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.PaidDebtOfIndebtedFirm.class)) {
                firm.getMessagesOfType(Messages.PaidDebtOfIndebtedFirm.class).forEach(msg -> {
                    firm.deposits += msg.debt;
                });
            }
        });
    }

    public static Action<Firms> receiveBailoutPackage() {
        return Action.create(Firms.class, firm -> {
            firm.getMessagesOfType(Messages.BailoutPackageMessage.class).forEach(msg -> {
                firm.priceOfGoods = msg.price;
                firm.wage = msg.wage;
                firm.deposits = 0;
            });
        });
    }

    public static Action<Firms> receiveBankruptcyMessage() {
        return Action.create(Firms.class, firm -> {
            firm.getMessagesOfType(Messages.BankruptcyMessage.class).forEach(msg -> {
                firm.deposits = 0;
                firm.isProductive = false;
                firm.targetProduction = 0;
            });
        });
    }


    public static Action<Firms> sendInfoToGetAvailableWorkers() {
        return Action.create(Firms.class, firm -> {
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmGetAvailableWorkers.class, (m, l) -> {
                m.sector = firm.sector;
            });
        });
    }

    public static Action<Firms> receiveAvailableWorkers() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.AvailableWorkersInYourSector.class)) {
                firm.getMessagesOfType(Messages.AvailableWorkersInYourSector.class).forEach(msg -> {
                            msg.workers = firm.availableWorkers;
                        }
                );
            }
        });
    }

    public static Action<Firms> sendPrice() {
        return Action.create(Firms.class, firm -> {
            if (firm.previousOutput > 0) {
                firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmsPrice.class, (m, l) -> {
                    m.price = firm.priceOfGoods;
                    m.output = firm.stock;
                });
            }
        });
    }

    public static Action<Firms> adjustPriceProduction() {
        return Action.create(Firms.class, firm -> {
            if (firm.previousOutput > firm.demand) {
                firm.targetProduction = firm.previousOutput + Math.min(firm.getGlobals().etta_plus * (firm.demand - firm.previousOutput), firm.getGlobals().mu * firm.availableWorkers);
                if (firm.priceOfGoods < firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d + firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            } else if (firm.previousOutput < firm.demand) {
                firm.targetProduction = Math.max(0.0d, firm.previousOutput - (firm.getGlobals().etta_minus * (firm.previousOutput - firm.demand)));
                if (firm.priceOfGoods > firm.averagePrice) {
                    firm.priceOfGoods = firm.priceOfGoods * (1.0d - firm.getGlobals().gamma_p * firm.getPrng().uniform(0, 1).sample());
                }
            }
            firm.demand = 0;
        });
    }

    public static Action<Firms> UpdateVacancies() {
        return Action.create(Firms.class, firm -> {
            int targetWorkers = (int) Math.ceil(firm.targetProduction); //assuming a one to one relationship
            if (targetWorkers > firm.workers) {
                firm.vacancies = targetWorkers - firm.workers;
            } else if (targetWorkers < firm.workers) {
                firm.isHiring = false;
                firm.vacancies = 0;
                firm.workersToBeFired = Math.abs(firm.workers - targetWorkers);
            }
        });
    }

    public static Action<Firms> FireWorkers() {
        return Action.create(Firms.class, firm -> {

            // a treemap sorts the key values in ascending order, i.e. from lower to higher productivity
            TreeMap<Double, Long> workers = new TreeMap<>();
            if (firm.hasMessageOfType(Messages.JobCheck.class)) {
                firm.getMessagesOfType(Messages.JobCheck.class).forEach(msg -> {
                    workers.put(msg.productivity, msg.getSender());
                });
            }

            if (!firm.isHiring) {
                // if it wants to fire all the workers the firm sends a message to all its employees
                // reducing computational complexity by doing it this way
                if (firm.workersToBeFired == firm.workers) {
                    firm.getLinks(Links.FirmToWorkerLink.class).send(Messages.Fired.class);
                }
            } else {
                int firedWorkers = 0;
                while (firedWorkers < firm.workersToBeFired) {
                    double workerKey = (double) workers.keySet().toArray()[firedWorkers];
                    long workerID = workers.get(workerKey);
                    firm.send(Messages.Fired.class).to(workerID);
                    firedWorkers++;
                }
            }
        });
    }

    public static Action<Firms> UpdateFirmSize() {
        return Action.create(Firms.class, firm -> {
            if (firm.workers >= 10) {
                firm.sizeOfCompany = 0;
            } else if (firm.workers > 10 && firm.workers <= 100) {
                firm.sizeOfCompany = 1;
            } else {
                firm.sizeOfCompany = 2;
            }
        });
    }
}

