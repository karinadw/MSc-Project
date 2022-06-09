package macro_financial_framework;

import com.google.errorprone.annotations.Var;
import org.apache.arrow.flatbuf.Bool;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Random;

public class Workers extends Agent<MacroFinancialModel.Globals> {

//    public static boolean isEmployed = false;  // everyone starts by being unemployed

//    public static int worker_sector;
//    public static boolean isEmployed = false;
//
////    @Variable
////    public int worker_sector_debug;
//
//
//    void divide_into_sectors() {
//        Random rand = new Random();
//        Workers.worker_sector = rand.nextInt(4);
////        worker_sector_debug = worker_sector;
//    }


    @Variable public int worker_sector;

    @Override
    public void init() {
        Random rand = new Random();
        worker_sector = rand.nextInt(3);
    }

//    void applyForJob() {
//        getLinks(Links.FirmLink.class).send(Messages.JobApplication.class, worker_sector);
//    }

    private static Action<Workers> action(SerializableConsumer<Workers> consumer) {
        return Action.create(Workers.class, consumer);
    }

    public static Action<Workers> applyForJob() {
        return action(
                worker -> {
                    worker.send_working_sector();
                });
    }

    private void send_working_sector(){
        getLinks(Links.FirmLink.class).send(Messages.JobApplication.class, worker_sector);
    }
}


