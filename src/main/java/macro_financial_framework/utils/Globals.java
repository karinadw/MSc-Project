package macro_financial_framework.utils;

import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Input;

import java.util.HashMap;


public class Globals extends GlobalState {

    @Input(name = "Number of Firms")
    public long nbFirms = 100;

    @Input(name = "Size of Workforce")
    public long nbWorkers = 1000;

    @Input(name = "Percentage Firms Micro-Small")
    public double percentMicroSmallFirms = 0.95;

    @Input(name = "Number of Sectors")
    public int nbSectors = 21;

    @Input
    public double c = 0.2d; // this is for calculating the consumption budget

    @Input
    //TODO: refactor this as I have named the productivity of a firm alpha
    public double delta = 0.02d; // this is used for calculating the dividend for investors (dividend = alpha * profit)

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
    @Input(name="gammaW")
    public double gamma_w = 0.05d;

    @Input
    public int nbGoods = nbSectors;

    public HashMap<Integer, Long> goodExchangeIDs;

    @Input(name="f")
    public double f = 1.0d;

    @Input(name="phi")
    public double phi = 0.1d;

    @Input
    public double nbExclusiveGoods = 0.2d; // its a percentage
    @Input
    public double percentageWealthyHouseholds = 0.01;
    @Input
    public long initialSaving = 10000;
    @Input
    public long initialSavingRich = 35000;
    @Input
    public long initialDeposits = 10000;
    @Input
    public int deposistsMultiplier = 2;
    public int totalVacancies;

}

