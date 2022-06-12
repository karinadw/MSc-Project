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

        createLongAccumulator("firm_vacancies");

        registerAgentTypes(Firms.class, Workers.class, LabourMarket.class);
        registerLinkTypes(Links.WorkersLink.class, Links.FirmsLink.class);
    }

    @Override
    public void setup() {

        Group<Firms> simpleFirmGroup = generateGroup(Firms.class, getGlobals().nbFirms);
        Group<Workers> simpleWorkersGroup = generateGroup(Workers.class, getGlobals().nbWorkers);
        Group<LabourMarket> labourMarketGroup =generateGroup(LabourMarket.class, 1);

        simpleWorkersGroup.fullyConnected(labourMarketGroup, Links.WorkersLink.class);
        simpleFirmGroup.fullyConnected(labourMarketGroup, Links.FirmsLink.class);

        labourMarketGroup.fullyConnected(simpleFirmGroup, Links.FirmsLink.class);
        labourMarketGroup.fullyConnected(simpleWorkersGroup, Links.WorkersLink.class);


        super.setup();
    }

    @Override
    public void step() {
        super.step();


        run(Firms.initVariables(), Workers.applyForJob(), Firms.sendVacancies(), LabourMarket.getFirmVacancies(), LabourMarket.readApplications(), LabourMarket.FirmsHire(), Workers.updateAvailability(), Firms.updateVacancies());
//        run(Firms.initVariables(), Firms.sendVacancies());
    }


}

