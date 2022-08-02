package SimpleEconomyModel.utils;

public class HealthyFirmAccount {

    public double deposits;

    public double price;

    public double wage;

    public HealthyFirmAccount(double deposits, double price, double wage) {
        this.deposits = deposits;
        this.price = price;
        this.wage = wage;
    }

    public void updateDeposits(double val) {
        this.deposits += val;
    }
}
