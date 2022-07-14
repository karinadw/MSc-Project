package macro_financial_framework;

import simudyne.core.abm.AgentBasedModel;
import simudyne.core.abm.GlobalState;
import simudyne.core.abm.Group;
import simudyne.core.abm.Split;
import simudyne.core.annotations.Input;

import java.util.ArrayList;
import java.util.HashMap;


public class MacroFinancialModel extends AgentBasedModel<MacroFinancialModel.Globals> {

    //Globals stores all of your variables and data structures that you want your agents to be able to access
    //Store information here that is system-level knowledge (ie - # of Agents or static variables)
    public static class Globals extends GlobalState {

        @Input
        public long nbFirms = 50;

        @Input
        public long nbWorkers = 100;

        @Input
        public int nbSectors = 10;

        @Input
        public double c = 0.025d; // this is for calculating the consumption budget

        @Input
        //TODO: refactor this as I have named the productivity of a firm alpha
        public double alpha = 0.02d; // this is used for calculating the dividend for investors (dividend = alpha * profit)

        // TODO: copied the number from Mark 0 model
        @Input(name = "ettaPlus")
        public double etta_plus = 0.416d;

        // TODO: copied the number from Mark 0 model
        @Input(name = "ettaMinus")
        public double etta_minus = 0.12d;

        // TODO: copied the number from Mark 0 model
        @Input(name = "gammaP")
        public double gamma_p = 0.05d;

        // TODO: copied the number from Mark 0 model
        @Input(name="Theta")
        public double Theta = 1.5d;

        @Input(name = "mu")
        public double mu = 1.0;

        @Input
        public int nbGoods = 3;

        public HashMap<Integer, Long> goodExchangeIDs;

        @Input(name="f")
        public double f = 1.0d;

        @Input(name="phi")
        public double phi = 0.1d;

    }

    @Override
    public void init() {

        registerAgentTypes(Firms.class,
                Households.class,
                Economy.class,
                GoodsMarket.class);

        registerLinkTypes(Links.HouseholdToEconomy.class,
                Links.FirmToEconomyLink.class,
                Links.FirmToWorkerLink.class,
                Links.WorkerToFirmLink.class,
                Links.FirmToInvestorLink.class,
                Links.InvestorToFirmLink.class,
                Links.FirmToBuyerLink.class,
                Links.EconomyToFirm.class,
                Links.GoodsMarketToEconomy.class);
    }

    int i = 0;
    int householdNumber = 0;

    @Override
    public void setup() {

        getGlobals().goodExchangeIDs = new HashMap<>();

        Group<GoodsMarket> goodsMarket = generateGroup(GoodsMarket.class, getGlobals().nbGoods, goodsVariable -> {
            goodsVariable.householdsDemandingGoods = new ArrayList<>();
            goodsVariable.firmsSupplyingGoods = new ArrayList<>();
            goodsVariable.goodTraded = i; // the ID of the good when the agent is being created
//            goodsVariable.competitive = Math.random() < 0.5; // randomly sets it as a competitive good or not
            getGlobals().goodExchangeIDs.put(i, goodsVariable.getID()); // so that the good can be accessed from globals instead of adding links

            int exclusiveGoods = (int) Math.ceil(0.2 * getGlobals().nbGoods);
            if (i >= exclusiveGoods) {
                goodsVariable.competitive = false; // goods only purchased by very wealthy individuals
            } else {
                goodsVariable.competitive = true; // this is common goods accesible to everyone, more common goods
            }
            i++;
        });


        Group<Firms> FirmGroup = generateGroup(Firms.class, getGlobals().nbFirms, firm -> {
            firm.sector = firm.getPrng().getNextInt(getGlobals().nbSectors - 1);
            firm.firingRate = 0.05;
            firm.sizeOfCompany = firm.getPrng().getNextInt(2); //start-up: 0, medium-sized: 1, large company: 2
        });


        Group<Households> HouseholdGroup = generateGroup(Households.class, getGlobals().nbWorkers, household -> {
            household.sector_skills = household.getPrng().getNextInt(getGlobals().nbSectors - 1);  // random sector skills applied to the workers
            household.accumulatedSalary = 0;
            // skewed distribution of wealth
            // reference for number used for saving -> initial wealth: https://www.ons.gov.uk/peoplepopulationandcommunity/personalandhouseholdfinances/incomeandwealth/bulletins/distributionofindividualtotalwealthbycharacteristicingreatbritain/april2018tomarch2020
            //TODO: update this to work for more than 3 goods
            household.budget = new HashMap<Integer, Double>();

            if (householdNumber < (getGlobals().nbWorkers - Math.ceil(0.1 * getGlobals().nbWorkers))) {
                // common individuals
                household.rich = false;
                household.savings = household.getPrng().uniform(100000.00, 200000.00).sample();
                double moneyToSpend = 0.025 * household.savings;
                int exclusiveGoods = (int) Math.ceil(0.2 * getGlobals().nbGoods);
                for (int j = 0; j < getGlobals().nbGoods - exclusiveGoods; j++) {
                    household.budget.put(j, moneyToSpend / (getGlobals().nbGoods) - exclusiveGoods);
                }
            } else {
                // wealthy individuals
                household.rich = true;
                household.savings = household.getPrng().uniform(200000.00, 400000.00).sample();

                double moneyToSpend = 0.025 * household.savings;
                // wealthy individuals spend on luxury goods as well
                // they spend on all goods
                for (int j = 0; j < getGlobals().nbGoods; j++) {
                    household.budget.put(j, moneyToSpend / getGlobals().nbGoods);
                }
            }
            householdNumber++;
            household.unemploymentBenefits = (61.05 + 77.00); // average of above and below 24 years, not dividing by 2 because this is received every 2 weeks.
            // TODO: check if these numbers make sense
            household.productivity = household.getPrng().uniform(0.5, 1).sample();
        });


        Group<Economy> Economy = generateGroup(Economy.class, 1, market -> {
            market.firmsHiring = new ArrayList<>();
            market.availableWorkers = new ArrayList<>();
//            market.householdsDemandingGoods = new ArrayList<>();
//            market.firmsSupplyingGoods = new ArrayList<>();
            market.priceOfGoods = new HashMap<Double, Double>();
            market.healthyFirmAccountMap = new HashMap<Long, HealthyFirmAccount>();
            market.indebtedFirmsMap = new HashMap<>();
            market.bankruptFirmsArray = new ArrayList<>();
            market.bailedOutFirmsMap = new HashMap<>();
        });

        HouseholdGroup.fullyConnected(Economy, Links.HouseholdToEconomy.class);
        FirmGroup.fullyConnected(Economy, Links.FirmToEconomyLink.class);
        Economy.fullyConnected(FirmGroup, Links.EconomyToFirm.class);
        Economy.fullyConnected(goodsMarket, Links.GoodsMarketToEconomy.class);

        super.setup();
    }

    @Override
    public void step() {
        super.step();

        // Initial settings that do not need to get repeated throughout
        if (getContext().getTick() == 0) {

            // firms set their vacancies according to their size
            run(Firms.SetVacancies());

            //the firm sets the prices of the goods it produces
            run(Firms.SetPriceOfGoods());

            // set sector specific wages and sector specific pricing of goods
            run(Firms.sendVacancies(), Economy.SetFirmProperties(), Firms.SetSectorSpecifics());

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
        }

        // workers apply for jobs and firms that have vacancies hire
        // firms decide what good they produce depending on the sector they're in
        // TODO: check the flow of actions here, doesn't make sense as of now
        run(
                Split.create(
                        // if the worker is unemployed, the worker applies for a job. Received by the economy
                        Households.applyForJob(),
                        // if the firm is hiring, vacancies are sent to the economy
                        Firms.sendVacancies()
                ),
                        Economy.MatchFirmsAndWorkers(),
                Split.create(
                        // if the worker has been employed it updates its status
                        Households.updateAvailability(),
                        Firms.updateVacancies()
                )
        );

//        // after each hiring round, unemployment is calculated
//        run(Households.SendUnemployment(), Economy.calculateUnemployment());

        // the productivity of the firm is dependant on the productivity of the workers
        run(Households.sendProductivity(), Firms.CalculateFirmProductivity());

        // Firms produce their good according to the productivity of the firm, the number of workers and the size of the firm
        run(Firms.FirmsProduce());

        // calculates the average price of products and sends it to the firms
        // this is needed for the firm to update its strategy
        // change in average price is used to calculate inflation
        run(Firms.sendPrice(), Economy.GetPrices());
        run(Economy.CalculateAndSendAveragePrice(), Firms.GetAveragePrice());

        // calculates inflation
        // not done in the first tick as there is no information in product pricing yet
        if (getContext().getTick() > 0) {
            run(Economy.CalculateInflation());
        }

        // workers get paid the wage offered by their firm and investors get paid dividends
        run(Firms.payWorkers(), Households.receiveIncome());

        run(
                Split.create(
                        Households.sendDemand(),
                        Firms.sendSupply()),
                GoodsMarket.matchSupplyAndDemand(),
                Split.create(
                        Firms.receiveDemand(),
                        Households.updateFromPurchase()
                )
        );


        //TODO: check if all the functions below are right
        run(
                Split.create(
                        Firms.sendInfoToGetAvailableWorkers(),
                        Households.SendUnemployment()
                ),
                Economy.calculateUnemploymentAndAavailableWorkers(),
                Firms.receiveAvailableWorkers()
        );

        // after households purchase, the update their consumption budget for each goog
        run(Households.updateConsumptionBudget());


        // Firm accounting
        run(Firms.Accounting());

        //firms pay out dividends to investors if earnings and profits are positive
        run(Firms.payInvestors(), Households.getDividends());


        //update the target production to meet the demand
        run(Firms.adjustPriceProduction());

        // updates firms' vacancies according to the new target production
        run(Firms.UpdateVacancies());

        // with the updated vacancies, the firms hire if they don't need anymore workers
        // workers that had already applied can now apply in the next hiring round
        run(Households.JobCheck(), Firms.FireWorkers(), Households.CheckIfFired());
        run(Households.UnemployedWorkerCanApply());

        // firms can change sizes depending on the employees it has
        run(Firms.UpdateFirmSize());
    }

}


