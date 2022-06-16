package macro_financial_framework;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class Economy extends Agent<MacroFinancialModel.Globals> {

    //    public HashMap<Long, Integer> applicants;
//    public HashMap<Long, Integer> FirmVacancies;
//    public HashMap<Long, Integer> FirmSectors;
//
//    public LabourMarket(){
//        applicants = new HashMap<>();
//        FirmVacancies = new HashMap<>();
//        FirmSectors = new HashMap<>();
//    }
    public List<WorkerID> availableWorkers;
    public List<FirmID> firmsHiring;

    public HashMap<Long, Double> priceOfGoods;

    public Economy(){
        priceOfGoods = new HashMap<Long, Double>();
    }


    public static Action<Economy> AssignInvestorToFirm(){
        return Action.create(Economy.class, market -> {
            List<Long> allHouseholds = new ArrayList<Long>();
            market.getMessagesOfType(Messages.ApplyForInvestor.class).forEach(msg -> {
                allHouseholds.add(msg.getSender());
            });

            List<Long> allFirms = new ArrayList<Long>();
            market.getMessagesOfType(Messages.FindInvestor.class).forEach(msg -> {
                allFirms.add(msg.getSender());
            });

            for(int i = 0; i < allFirms.size(); i++){
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

    public static Action<Economy> SetFirmWages(){
        return Action.create(Economy.class, market -> {
            // creating a hashmap tp store all the sectors and their corresponding wage
            HashMap<Integer, Double> sectorWages = new HashMap<Integer, Double>();
            int numberOfSectors = market.getGlobals().nbSectors; // not adding -1 and instead keeping i<numberOfSector instead of <=
            for(int i = 0; i < numberOfSectors; i++){
                int sector = i;
                double wage = market.getPrng().uniform(1000.00,3000.00).sample();
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

    public static Action<Economy> setFirmPriceOfGoods(){
        return Action.create(Economy.class, market -> {
           market.getMessagesOfType(Messages.priceOfGoods.class).forEach(msg -> {
               market.priceOfGoods.put(msg.getSender(), msg.price);
           });
        });
    }



    public static Action<Economy> MatchFirmsAndWorkers() {
        return Action.create(Economy.class, market -> {
            market.getMessagesOfType(Messages.JobApplication.class).forEach(msg -> {
                market.availableWorkers.add(new WorkerID(msg.getSender(), msg.getBody()));
            });
            market.firmsHiring.clear();  // to account for new vacancies in case of firing
            market.getMessagesOfType(Messages.FirmInformation.class).forEach(mes -> {
                market.firmsHiring.add(new FirmID(mes.getSender(), mes.sector, mes.vacancies));
            });
            market.firmsHiring.forEach(firm -> {
                int sector = firm.sector;
                int vacancies = firm.vacancies;
                for (int x = 0; x < vacancies; x++) {
                    Optional<WorkerID> potentialWorker = market.availableWorkers.stream().filter(w -> w.sector == sector).findAny();
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
                        //TODO: CHECK LOGIC TO SEE IF WE NEED TO UPDATE VACANCIES
//                        firm.vacancies = vacancies;
                        break;
                    }
                }

            });

        });
    }

}



