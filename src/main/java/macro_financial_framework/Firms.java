package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Input;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Firms extends Agent<MacroFinancialModel.Globals> {

    @Variable
    public int vacancies;

    public int sizeOfCompany;
    @Variable
    public int sector;

    @Variable
    public double wage;

    @Input
    public double firingRate;

    public double priceOfGoods;

    public double productivity;


    public static Action<Firms> SetVacancies(){
        return Action.create(Firms.class, firm -> {
            // set vacancies according to firm size
           if (firm.sizeOfCompany == 0){
               firm.vacancies = (int) firm.getPrng().uniform(1, 10).sample();
           } else if (firm.sizeOfCompany == 1){
               firm.vacancies = (int) firm.getPrng().uniform(10, 100).sample();
           } else {
               firm.vacancies = (int) firm.getPrng().uniform(100, 1000).sample();
           }
        });
    }
    public static Action<Firms> SetSectorSpecificWages(){
        return Action.create(Firms.class, firm -> {
            if (firm.hasMessageOfType(Messages.FirmWage.class)){
                firm.getMessagesOfType(Messages.FirmWage.class).forEach(msg -> {
                    if (firm.sizeOfCompany == 0) {
                        firm.wage = msg.wage + firm.getPrng().uniform(-200.00, -100.00).sample(); // smaller companies pay a wage smaller than the average
                    } else if (firm.sizeOfCompany == 1){
                        firm.wage = msg.wage + firm.getPrng().uniform(-100.00, 100.00).sample(); // medium sized companies pay a wage that can be slightly lower or higher than average
                    } else {
                        firm.wage = msg.wage + firm.getPrng().uniform(100.00, 200.00).sample(); // big companies pay a wage that is higher than the average for the sector
                    }
                });
            }
        });
    }

    public static Action<Firms> SetPriceOfGoods(){
        return Action.create(Firms.class, firm -> {
            double price = firm.getPrng().getNextDouble(1000.00);
            firm.getLinks(Links.FirmToEconomyLink.class).send(Messages.priceOfGoods.class, (priceOfGoods, firmToEconomyLink) -> {
                priceOfGoods.price = price;
            });
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
                });
            }
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
            while(i < numberOfWorkersToFire){
                double workerProductivity = workersProductivity.get(i); // if workers have a productivity above 0.9 they won't get fired
                if (workerProductivity< 0.9) {
                    firm.send(Messages.Fired.class).to(workersID.get(i));
                    firm.removeLinksTo(workersID.get(i));
                    firm.vacancies++;
                    i++;
                }
            }

        });
    }


}

