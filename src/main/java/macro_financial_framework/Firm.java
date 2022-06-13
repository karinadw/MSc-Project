package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;

public class Firm extends Agent<MacroFinancialModel.Globals> {

    @Variable
    public int vacancies;

    public int sector;


//    public static Action<Firm> initVariables() {
//        return Action.create(Firm.class, firm -> {
//            firm.sector = firm.getPrng().getNextInt(firm.getGlobals().nbSectors);
////            firm.vacancies = firm.getPrng().getNextInt(10);
//            firm.vacancies = 10;
//        });
//    }

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
                    firm.addLink(msg.getSender(), Links.FirmToWorkerLink.class);
                    firm.vacancies--;
                });
            }
        });
    }


}

