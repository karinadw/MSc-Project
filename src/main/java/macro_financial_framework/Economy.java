package macro_financial_framework;

//import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

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

    @Variable
    public double wealth_economy = 0;


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

    public static Action<Economy> receiveFirmWage() {
        return Action.create(Economy.class, economy -> {
            economy.getMessagesOfType(Messages.WorkerPayment.class).forEach(msg -> {
                economy.wealth_economy += msg.wage;
            });
        });
    }

}



