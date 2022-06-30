package macro_financial_framework;

public class FirmSupplyInformation {
    public long ID;
//    public int sectorOfFirm;
    public long output;
    public double price;

    public FirmSupplyInformation(long ID, long output, double price) {
        this.ID = ID;
//        this.sectorOfFirm = sectorOfFirm;
        this.output = output;
        this.price = price;
    }
}
