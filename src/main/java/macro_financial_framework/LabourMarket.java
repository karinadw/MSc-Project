package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LabourMarket extends Agent<MacroFinancialModel.Globals> {

    public HashMap<Long, Integer> applicants;
    public HashMap<Long, Integer> FirmVacancies;
    public HashMap<Long, Integer> FirmSectors;

    public LabourMarket(){
        applicants = new HashMap<>();
        FirmVacancies = new HashMap<>();
        FirmSectors = new HashMap<>();
    }

    public static Action<LabourMarket> readApplications() {
        return Action.create(LabourMarket.class, market -> {
            market.getMessagesOfType(Messages.JobApplication.class).forEach(msg -> {
                market.applicants.put(msg.getSender(),msg.getBody());
            });
        });
    }

    public static Action<LabourMarket> getFirmVacancies() {
        return Action.create(LabourMarket.class, market -> {
            market.getMessagesOfType(Messages.FirmInformation.class).forEach(msg ->{
                market.FirmSectors.put(msg.getSender(), msg.getBody());
            });
            market.getMessagesOfType(Messages.FirmVacancies.class).forEach(msg ->{
                market.FirmVacancies.put(msg.getSender(), msg.getBody());
//                market.getLongAccumulator("firm_vacancies").add(msg.getBody());
        });
    });
    }

    public static Action<LabourMarket> FirmsHire() {
        return Action.create(LabourMarket.class, market -> {
            market.FirmVacancies.keySet().forEach(firmID -> {

                // check if firm has vacancies
                if (market.FirmVacancies.get(firmID) > 0) {

                    // if it does, get the sector in which it works
                    int sector = market.FirmSectors.get(firmID);

                    // iterate over all the workers until a match between firm sector and worker sector is found
                    AtomicInteger firmVacancies = new AtomicInteger(market.FirmVacancies.get(firmID));
                    AtomicInteger hired = new AtomicInteger(0);

                    market.applicants.keySet().forEach(workerID -> {
                        int sector_worker = market.applicants.get(workerID);
                        if (sector_worker == sector && hired.get() <= firmVacancies.get()) {
                            market.getLinksTo(firmID, Links.WorkersLink.class).send(Messages.WorkerHired.class);
                            market.getLinksTo(firmID, Links.FirmsLink.class).send(Messages.FirmHiredWorker.class);
                            firmVacancies.addAndGet(-1);
                            hired.addAndGet(+1);
                        }
                    });
                }
            });
        });
    }



}

