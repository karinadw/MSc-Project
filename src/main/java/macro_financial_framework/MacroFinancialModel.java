package macro_financial_framework;

import simudyne.core.abm.Action;
import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.annotations.Input;
import simudyne.core.graph.LongAccumulator;


public class MacroFinancialModel extends AgentBasedModel<MacroFinancialModel.Globals> {

    //Globals stores all of your variables and data structures that you want your agents to be able to access
    //Store information here that is system-level knowledge (ie - # of Agents or static variables)
    public static class Globals extends GlobalState {

        @Input
        public long nbFirms = 15;

        @Input
        public long nbWorkers  = 50;

        @Input
        public int nbSectors = 3;

    }

    @Override
    public void init() {

//        createLongAccumulator("workers in sector 1");
//        createLongAccumulator("workers in sector 2");
//        createLongAccumulator("workers in sector 3");
//        createLongAccumulator("firms in sector 1");
//        createLongAccumulator("firms in sector 2");
//        createLongAccumulator("firms in sector 3");

        registerAgentTypes(Firms.class, Workers.class);
        registerLinkTypes(Links.FirmLink.class);
    }

    @Override
    public void setup() {

        Group<Firms> simpleFirmGroup = generateGroup(Firms.class, getGlobals().nbFirms);
        Group<Workers> simpleWorkersGroup = generateGroup(Workers.class, getGlobals().nbWorkers);

        simpleWorkersGroup.fullyConnected(simpleFirmGroup, Links.FirmLink.class);
//        simpleFirmGroup.fullyConnected(simpleWorkersGroup, Links.FirmLink.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

//        run(
//                Action.create(
//                        Workers.class,
//                        workers -> {
//                            workers.applyForJob();
//                        }),
//                Action.create(
//                        Firms.class,
//                        firms -> {
//                            firms.hire();
////                            workers.employ();
//                        }));

        run(Workers.applyForJob(), Firms.hire());

    }


}

