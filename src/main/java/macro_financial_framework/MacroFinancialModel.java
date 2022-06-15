package macro_financial_framework;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Input;
import simudyne.core.graph.Link;

import java.util.ArrayList;


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

        registerAgentTypes(Firms.class, Households.class, Economy.class);
        registerLinkTypes(Links.WorkerToEconomyLink.class,
                Links.FirmToEconomyLink.class,
                Links.FirmToWorkerLink.class,
                Links.WorkerToFirmLink.class,
                Links.FirmToInvestorLink.class,
                Links.InvestorToFirmLink.class);
    }

    @Override
    public void setup() {

        Group<Firms> simpleFirmGroup = generateGroup(Firms.class, getGlobals().nbFirms, firm -> {
            firm.vacancies = (int) firm.getPrng().uniform(2, 10).sample();
            firm.sector = firm.getPrng().getNextInt(getGlobals().nbSectors - 1);
            firm.firingRate = 0.05;
        });
        Group<Households> simpleWorkersGroup = generateGroup(Households.class, getGlobals().nbWorkers, worker -> {
            worker.sector_skills = worker.getPrng().getNextInt(getGlobals().nbSectors - 1);  // random sector skills applied to the workers
            worker.wealth = 0;
            worker.productivity = worker.getPrng().getNextInt(10);
        });
        Group<Economy> labourMarketGroup = generateGroup(Economy.class, 1, market ->{
            market.firmsHiring = new ArrayList<>();
            market.availableWorkers = new ArrayList<>();
        });

        simpleWorkersGroup.fullyConnected(labourMarketGroup, Links.WorkerToEconomyLink.class);
        simpleFirmGroup.fullyConnected(labourMarketGroup, Links.FirmToEconomyLink.class);



        super.setup();
    }

    @Override
    public void step() {
        super.step();

        // dividing households into investors and workers
        run(
                Split.create(Households.ApplyForInvestor(),
                            Firms.FindInvestors()),
                Economy.AssignInvestorToFirm(),
                Split.create(
                        Households.DetermineStatus(),
                        Firms.AssignFirmInvestor()
                )
        );

        // workers apply for jobs and firms that have vacancies hire
        // firms set their wage according to the sector they're in
        run(
                Split.create(
                        Households.applyForJob(),
                        Firms.sendVacancies()),
                Split.create(
                        Economy.SetFirmWages(),
                        Economy.MatchFirmsAndWorkers()),
                Split.create(
                        Firms.SetSectorSpecificWages(),
                        Households.updateAvailability(),
                        Firms.updateVacancies()
                ));

        // workers get paid the wage offered by their firm and investors get paid dividends
        run(Firms.payWorkers(), Households.receiveSalary());

        // assuming 12 ticks represent a year
        // annually firms fire workers and workers update their availabilities
        if (getContext().getTick() % 12 == 0) {
            run(Households.AnnualCheck(), Firms.FireWorkers(), Households.CheckIfFired());
        }
    }


}

