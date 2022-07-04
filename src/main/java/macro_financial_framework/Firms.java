package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.List;

public class Firms extends Agent<MacroFinancialModel.Globals> {

    @Variable
    public int vacancies;
    @Variable
    public int workers;
    public int sizeOfCompany;
    @Variable
    public int sector;
    //    @Variable
//    public double deposits;
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
    public double production;
    public double targetProduction;
    public int good;


    public static Action<Firms> SetVacancies() {
        return Action.create(Firms.class, firm -> {
            // set vacancies according to firm size
            //TODO: check if logic makes sense
            if (firm.sizeOfCompany == 0) {
                firm.vacancies = (int) firm.getPrng().uniform(1, 10).sample();
            } else if (firm.sizeOfCompany == 1) {
                firm.vacancies = (int) firm.getPrng().uniform(10, 100).sample();
            } else {
                firm.vacancies = (int) firm.getPrng().uniform(100, 1000).sample();
            }
        });
    }

    public static Action<Firms> SetSectorSpecificWages() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.FirmWage.class)) {
                firm.getMessagesOfType(Messages.FirmWage.class).forEach(msg -> {
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

    public static Action<Firms> SetSectorSpecificGood() {
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.FirmGood.class)) {
                firm.getMessagesOfType(Messages.FirmGood.class).forEach(msg -> {
                    firm.good = msg.good;
                });
            }
        });
    }

    public static Action<Firms> SetPriceOfGoods() {
        return Action.create(Firms.class, firm -> {
            //TODO: check how to make logical assumptions about pricing
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
            firms.getLinks(Links.FirmToEconomyLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                msg.vacancies = firms.vacancies;
                msg.sector = firms.sector;
            });
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
            if (firm.sizeOfCompany == 0) {
                firm.stock = (long) Math.floor(firm.productivity * (firm.workers / 1));
            } else if (firm.sizeOfCompany == 1) {
                firm.stock = (long) Math.floor(firm.productivity * (firm.workers / 0.5));
            } else {
                firm.stock = (long) Math.floor(firm.productivity * (firm.workers / 0.25));
            }
        });
    }

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
            firm.production = firm.stock;

            if (firm.hasMessageOfType(Messages.HouseholdWantsToPurchase.class)) {
                firm.getMessagesOfType(Messages.HouseholdWantsToPurchase.class).forEach(purchaseMessage -> {
                    firm.demand += purchaseMessage.demand;
                    firm.stock -= purchaseMessage.bought;
                    // cash received from the goods sold -> without subtracting costs of production and payment to workers/investors
                    firm.earnings += firm.priceOfGoods;
                });
            }

            firm.inventory += firm.stock;
            firm.stock = 0;

        });
    }

//    public static Action<Firms> receiveDemandAndSell() {
//        return Action.create(Firms.class, firm -> {
//            // firm receives the demand for its product
//            // just storing the total demand for the product for strategy calculation
//            if (firm.hasMessageOfType(Messages.HouseholdWantsToPurchase.class)) {
//                firm.getMessagesOfType(Messages.HouseholdWantsToPurchase.class).forEach(msg -> {
//                    firm.demand += msg.demand;
//                });
//            }
//
//            // the initial stock of the product of that firm
//            // I am storing how much the firm produced, as when the selling occurs the stock will go down
//            // needed for various metrics and calculations
//            firm.production = firm.stock;
//
//            // iterate over all the
//            if (firm.hasMessageOfType(Messages.HouseholdWantsToPurchase.class)) {
//                firm.getMessagesOfType(Messages.HouseholdWantsToPurchase.class).forEach(msg -> {
//                    long demand = msg.demand;
////                    long availability = firm.output - demand;
//
//                    // if availability of the good (difference between stock and demand) is greater than 0
//                    // add link to the household
//                    // send the price of purchase
//                    //TODO: sort out what's happening with the inventory -> it should be used, in fact I should use LIFO
//                    if ((firm.stock - demand) >= 0 && demand > 0) {
//                        long amountSold = demand;
//                        firm.addLink(msg.HouseholdID, Links.FirmToBuyerLink.class);
//                        double price = firm.priceOfGoods * amountSold;
//                        firm.send(Messages.PurchaseCompleted.class, m -> {
//                            m.spent = price;
//                        }).to(msg.HouseholdID);
//                        firm.stock -= amountSold;
//                        firm.earnings += price;
//                        firm.removeLinksTo(msg.HouseholdID, Links.FirmToBuyerLink.class);
//                    } else if (firm.stock > 0 && demand > 0 && (firm.stock - demand) < 0) {
//                        long amountSold = firm.stock;
//                        firm.addLink(msg.HouseholdID, Links.FirmToBuyerLink.class);
//                        double price = firm.priceOfGoods * amountSold;
//                        firm.send(Messages.PurchaseCompleted.class, m -> {
//                            m.spent = price;
//                        }).to(msg.HouseholdID);
//                        firm.stock -= amountSold;
//                        firm.earnings += price;
//                        firm.removeLinksTo(msg.HouseholdID, Links.FirmToBuyerLink.class);
//                    }
//                });
//
//                // whatever has not been sold it now kept in inventory
//                firm.inventory += firm.stock;
//                firm.stock = 0;
//
//            }
//        });
//
//    }

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
            if (firm.profit > 0) {
                firm.dividend = firm.profit * firm.getGlobals().alpha;
                firm.getLinks(Links.FirmToInvestorLink.class).send(Messages.PayInvestors.class, (dividendPayment, firmToInvestorLink) -> {
                    dividendPayment.dividend = firm.dividend;
                });
            }
        });
    }

    public static Action<Firms> calculateProfits() {
        return Action.create(Firms.class, firm -> {
            // the profit is the difference between the cost of production (wage * nb of employees) and the products sold
            // might be negative as even if firms don't sell anything, they need to pay workers
            firm.profit += firm.earnings - (firm.wage * firm.workers) - firm.dividend;
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

            // first the firm needs to calculate what the new production goal will be according to the demand it received
//           firm.targetProduction = firm.production + Math.min(firm.getGlobals().etta_plus * (firm.demand - firm.output), firm.getGlobals().mu * firm.availableWorkers);
        });
    }

//    public static Action<Firms> AdjustVariable() {
//        return Action.create(Firms.class, firm -> {
//            // if the firm is productive, then re-calculate strategy
//            if (firm.productivity > 0) {
//                if (firm.production < firm.demand) {
//                    firm.targetProduction =
//
//                }
//            }
//        });
//    }


    public static Action<Firms> FireWorkers() {
        return Action.create(Firms.class, firm -> {
            // empty list to store the IDs of the workers of the firm
            List<Long> workersID = new ArrayList<Long>();
            List<Double> workersProductivity = new ArrayList<Double>();

            // storing all the IDs
            firm.getMessagesOfType(Messages.AnnualCheck.class).forEach(msg -> {
                workersID.add(msg.getSender());
            });

            // storing all the productivities
            firm.getMessagesOfType(Messages.AnnualCheck.class).forEach(msg -> {
                workersProductivity.add(msg.productivity);
            });

            // number of workers to be fired
            int numberOfWorkersToFire = (int) Math.ceil(workersID.size() * firm.firingRate);
            int i = 0;
            while (i < numberOfWorkersToFire) {
                double workerProductivity = workersProductivity.get(i); // if workers have a productivity above 0.9 they won't get fired
                if (workerProductivity < 0.9) {
                    firm.send(Messages.Fired.class).to(workersID.get(i));
                    firm.removeLinksTo(workersID.get(i));
                    firm.vacancies++;
                    i++;
                }
            }

        });
    }


}

