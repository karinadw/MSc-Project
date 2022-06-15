package macro_financial_framework;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Input;

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

        Group<Firms> FirmGroup = generateGroup(Firms.class, getGlobals().nbFirms, firm -> {
            firm.vacancies = (int) firm.getPrng().uniform(2, 10).sample();
            firm.sector = firm.getPrng().getNextInt(getGlobals().nbSectors - 1);
            firm.firingRate = 0.05;
        });
        Group<Households> HouseholdGroup = generateGroup(Households.class, getGlobals().nbWorkers, household -> {
            household.sector_skills = household.getPrng().getNextInt(getGlobals().nbSectors - 1);  // random sector skills applied to the workers
            household.wealth = 0;
            household.productivity = household.getPrng().getNextInt(10);
            household.unemploymentBenefits = (61.05 + 77.00) / 2; // average of above and below 24 years
        });
        Group<Economy> Economy = generateGroup(Economy.class, 1, market ->{
            market.firmsHiring = new ArrayList<>();
            market.availableWorkers = new ArrayList<>();
        });

        HouseholdGroup.fullyConnected(Economy, Links.WorkerToEconomyLink.class);
        FirmGroup.fullyConnected(Economy, Links.FirmToEconomyLink.class);



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
        run(Firms.payWorkers(), Households.receiveIncome());

        // assuming 12 ticks represent a year
        // annually firms fire workers and workers update their availabilities
        if (getContext().getTick() % 12 == 0) {
            run(Households.AnnualCheck(), Firms.FireWorkers(), Households.CheckIfFired());
        }
    }


}

