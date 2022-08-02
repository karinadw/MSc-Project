package SimpleEconomyModel.utils;

public class WorkerID {

    public long ID;
    public int sector;
    public double productivity;

    public WorkerID(long ID, int sector, double productivity) {
        this.ID = ID;
        this.sector = sector;
        this.productivity = productivity;
    }

}
