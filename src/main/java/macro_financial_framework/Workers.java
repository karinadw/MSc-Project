package macro_financial_framework;

import com.google.errorprone.annotations.Var;
import org.apache.arrow.flatbuf.Bool;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Random;

public class Workers extends Agent<MacroFinancialModel.Globals> {

    public int sector_skills;

    public enum Status{HIRED, UNEMPLOYED}

    public Status status = Status.UNEMPLOYED; //everyone starts by being unemployed

    @Override
    public void init() {
        sector_skills = getPrng().getNextInt(getGlobals().nbSectors);  // random sector skills applied to the workers
    }


    public static Action<Workers> applyForJob() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.status == Status.UNEMPLOYED){
                        worker. getLinks(Links.WorkersLink.class).send(Messages.JobApplication.class, worker.sector_skills);
                    }
                });
    }

    public static Action<Workers> updateAvailability() {
        return Action.create(Workers.class,
                worker -> {
                    if (worker.hasMessageOfType(Messages.WorkerHired.class)){
                        worker.status = Status.HIRED;
                    }

                });
    }
}


