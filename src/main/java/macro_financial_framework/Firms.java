package macro_financial_framework;

import org.checkerframework.checker.units.qual.A;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Firms extends Agent<MacroFinancialModel.Globals> {

    public int vacancies;

    public int sector;

    public Firms() {
        vacancies = 0;
        sector = 0;
    }

    public static Action<Firms> initVariables(){
        return Action.create(Firms.class, firm -> {
            firm.sector = firm.getPrng().getNextInt(firm.getGlobals().nbSectors);
//            firm.vacancies = firm.getPrng().getNextInt(10);
            firm.vacancies = 10;
        });
    }

    public static Action<Firms> sendVacancies(){
        return Action.create(Firms.class, firms -> {
            firms.getLinks(Links.FirmsLink.class).send(Messages.FirmVacancies.class, firms.vacancies);
            firms.getLongAccumulator("firm_vacancies").add(firms.vacancies);
            firms.getLinks(Links.FirmsLink.class).send(Messages.FirmInformation.class, firms.sector);
        });
    }

    public static Action<Firms> updateVacancies(){
        return Action.create(Firms.class, firms -> {
            firms.getMessagesOfType(Messages.FirmHiredWorker.class).forEach(msg -> {
                firms.vacancies -= 1;
            });
            });
        };

//    private int read_job_app() {
//        return getMessageOfType(Messages.JobApplication.class).getBody();
//    }
//
//    private void hired () {
//        getLinks(Links.FirmLink.class).send(Messages.Hired.class);
//    }
//
//    private void rejected () {
//        getLinks(Links.FirmLink.class).send(Messages.Job_update.class, 0);
//    }



}

