package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Firm extends Agent<MacroFinancialModel.Globals> {

    @Variable
    public int vacancies;

    public int sector;

    @Variable
    public double wage;

    public static Action<Firm> sendVacancies() {
        return Action.create(Firm.class, firms -> {
            firms.getLinks(Links.FirmToLabourMarketLink.class).send(Messages.FirmInformation.class, (msg, link) -> {
                msg.vacancies = firms.vacancies;
                msg.sector = firms.sector;
            });
//            firms.getLongAccumulator("firm_vacancies").add(firms.vacancies);
        });
    }

    public static Action<Firm> updateVacancies() {
        return Action.create(Firm.class, firm -> {
            if (firm.hasMessageOfType(Messages.NewEmployee.class)) {
                firm.getMessagesOfType(Messages.NewEmployee.class).forEach(msg -> {
                    firm.addLink(msg.workerID, Links.FirmToWorkerLink.class);
                    firm.vacancies--;
                });
            }
        });
    }

    public static Action<Firm> payWorkers() {
        return Action.create(Firm.class, firm -> {
            firm.getLinks(Links.FirmToWorkerLink.class).send(Messages.WorkerPayment.class, (msg, link) -> {
                msg.wage = firm.wage;
            });
        });
    }

}

