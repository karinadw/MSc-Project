package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

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
                for (int x = 0; x < vacancies; x++){
                    Optional<WorkerID> potentialWorker = market.availableWorkers.stream().filter(w -> w.sector == sector).findAny();
                    if (potentialWorker.isPresent()){

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

//    public static Action<LabourMarket> FirmsHire() {
//        return Action.create(LabourMarket.class, market -> {
//            market.FirmVacancies.keySet().forEach(firmID -> {
//
//                // check if firm has vacancies
//                if (market.FirmVacancies.get(firmID) > 0) {
//
//                    // if it does, get the sector in which it works
//                    int sector = market.FirmSectors.get(firmID);
//
//                    // iterate over all the workers until a match between firm sector and worker sector is found
//                    AtomicInteger firmVacancies = new AtomicInteger(market.FirmVacancies.get(firmID));
//                    AtomicInteger hired = new AtomicInteger(0);
//
//                    market.applicants.keySet().forEach(workerID -> {
//                        int sector_worker = market.applicants.get(workerID);
//                        if (sector_worker == sector && hired.get() <= firmVacancies.get()) {
//                            market.getLinksTo(firmID, Links.WorkerToLabourMarketLink.class).send(Messages.WorkerHired.class);
//                            market.getLinksTo(firmID, Links.FirmToLabourMarketLink.class).send(Messages.FirmHiredWorker.class);
//                            firmVacancies.addAndGet(-1);
//                            hired.addAndGet(+1);
//                        }
//                    });
//                }
//            });
//        });
//    }


}

