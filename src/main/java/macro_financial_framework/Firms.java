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
    public double productivity;
    @Variable
    public long stock;
    @Variable
    public int demand;
    @Variable
    public long inventory = 0;
    @Variable
    public double earnings = 0.0d;
    public double dividend = 0;
    public double previousOutput;
    public double targetProduction;
    public int good;
    public double deposits;
    public int availableWorkers;
    public double averagePrice;
    public boolean isHiring = true;
    public int workersToBeFired = 0;
    public int goodNeededForProduction;

    public static Action<Firms> SetVacancies() {
        return Action.create(Firms.class, firm -> {
            // set vacancies according to firm size
            // TODO: check if logic makes sense
            // potential resources: https://www.statista.com/statistics/676671/employees-by-business-size-uk/
            if (firm.sizeOfCompany == 0) {
                firm.vacancies = (int) firm.getPrng().uniform(1, 10).sample();
            } else if (firm.sizeOfCompany == 1) {
                firm.vacancies = (int) firm.getPrng().uniform(10, 100).sample();
            } else {
                firm.vacancies = (int) firm.getPrng().uniform(100, 1000).sample();
            }
        });
    }

    public static Action<Firms> SetSectorSpecifics() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.FirmProperties.class)) {
                firm.getMessagesOfType(Messages.FirmProperties.class).forEach(msg -> {
                    firm.good = msg.good;
                    firm.goodNeededForProduction = msg.goodToPurchase;
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
            if (firms.isHiring) {
                firms.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                    msg.vacancies = firms.vacancies;
                    msg.sector = firms.sector;
                });
            }
        });
    }

    public static Action<Firms> updateVacancies() {
        // hires workers and adjusts vacancies
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.NewEmployee.class)) {
                firm.getMessagesOfType(Messages.NewEmployee.class).forEach(msg -> {
                    firm.addLink(msg.workerID, Links.FirmToWorkerLink.class);
                    firm.vacancies--;
                    firm.workers += 1;
                });
            }
        });
    }


    public static Action<Firms> CalculateFirmProductivity() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.Productivity.class)) {
                firm.getMessagesOfType(Messages.Productivity.class).forEach(msg -> {
                    firm.productivity += msg.productivity;
                });
                firm.productivity /= firm.workers;

            // if the firm doesn't have workers, it's productivity is set to 0
            } else {
                firm.productivity = 0;
            }
        });
    }

    public static Action<Firms> FirmsProduce() {
        return Action.create(Firms.class, firm -> {
            // depending on the size of the firm -> workers can produce more or less goods
            // this is assuming that larger companies have more facilities
            //TODO: check if this is a fair assumption
            firm.stock = (long) Math.floor(firm.productivity * firm.workers);
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
                        firm.earnings += firm.priceOfGoods;
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

    public static Action<Firms> payInvestors() {
        // pays the investors if the firm is generating positive profits
        return Action.create(Firms.class, firm -> {
            if (firm.profit > 0 && firm.deposits > 0) {
                firm.dividend = firm.profit * firm.getGlobals().alpha;
                firm.profit -= firm.dividend;
                firm.getLinks(Links.FirmToInvestorLink.class).send(Messages.PayInvestors.class, (dividendPayment, firmToInvestorLink) -> {
                    dividendPayment.dividend = firm.dividend;
                });
            }
        });
    }

    public static Action<Firms> GetAveragePrice() {
        return Action.create(Firms.class, firm -> {
            firm.getMessagesOfType(Messages.AveragePrice.class).forEach(msg -> {
                        firm.averagePrice = msg.averagePrice;
                    }
            );
        });
    }

    public static Action<Firms> Accounting() {
        return Action.create(Firms.class, firm -> {
            // the profit is the difference between the cost of production (wage * nb of employees) and the products sold
            // might be negative as even if firms don't sell anything, they need to pay workers
            // TODO: haven't added health of a firm (check if its needed or not)
            firm.profit = firm.earnings - (firm.wage * firm.workers);
            firm.deposits += firm.demand;
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
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmsPrice.class, (m, l) -> {
                m.price = firm.priceOfGoods;
                m.output = firm.stock;
            });
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

            if (!firm.isHiring){
                // if it wants to fire all the workers the firm sends a message to all its employees
                // reducing computational complexity by doing it this way
                if (firm.workersToBeFired == firm.workers){
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
           if (firm.workers >= 10){
               firm.sizeOfCompany = 0;
           } else if (firm.workers > 10 && firm.workers <= 100){
               firm.sizeOfCompany = 1;
           } else {
               firm.sizeOfCompany = 2;
           }
        });
    }
}

