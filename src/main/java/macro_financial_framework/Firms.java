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

    public int sector;

    @Variable
    public double wage;

    @Input
    public double firingRate;

    public static Action<Firms> sendVacancies() {
        return Action.create(Firms.class, firms -> {
            firms.getLinks(Links.FirmToLabourMarketLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                msg.vacancies = firms.vacancies;
                msg.sector = firms.sector;
            });
        });
    }

    public static Action<Firms> updateVacancies() {
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
            // storing all the IDs
            firm.getMessagesOfType(Messages.AnnualCheck.class).forEach(msg -> {
                workersID.add(msg.getSender());
            });

            // number of workers to be fired
            int numberOfWorkersToFire = (int) Math.ceil(workersID.size() * firm.firingRate);
            System.out.println(numberOfWorkersToFire);
            int i = 0;
            while(i < numberOfWorkersToFire){
                firm.send(Messages.Fired.class).to(workersID.get(i));
                firm.removeLinksTo(workersID.get(i));
                firm.vacancies ++;
                i++;
            }

        });
    }


}

