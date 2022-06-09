package macro_financial_framework;

import org.checkerframework.checker.units.qual.A;
import simudyne.core.abm.Action;
import simudyne.core.abm.Agent;
import simudyne.core.abm.GlobalState;
import simudyne.core.annotations.Variable;
import simudyne.core.functions.SerializableConsumer;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Firms extends Agent<MacroFinancialModel.Globals> {

    /// THIS CODE IS TO CONNECT ALL THE FIRMS -> FOR SIMPLICITY AS OF NOW WE WILL ASSUME THAT FIRMS ONLY INTERACT WITH HOUSEHOLDS

//    @Variable
//    public double wealth;
//    public double money_to_buy_goods; // this is the money that a firm needs to spend on external goods/materials for the products
//    public double production_cost; // cost of the production of final products
//    public double total_money_of_products_sold; // this is the total amount of money made from selling the goods, not price pero product
//
//    @Override
//    public void init() {
//        wealth = getPrng().getNextDouble(10000);
//        money_to_buy_goods = getPrng().getNextDouble(100);
//        production_cost = getPrng().getNextDouble(100);
//        total_money_of_products_sold = getPrng().getNextDouble(1000);
//    }
//
//    public static Action<Firms> buyGoods =
//            Action.create(Firms.class, Firms -> {
//
//                // buying materials/goods from other firms to produce their own products
//                if (Firms.wealth > 0) {
//                    List<Links.FirmLink> connections = Firms.getLinks(Links.FirmLink.class).getList();
//                    Collections.shuffle(connections, Firms.getPrng().getRandom());
//
//                    Firms.send(Messages.PaymentMessage.class
//                    ).to(connections.get(0).getTo());
//
//                    Firms.wealth -= Firms.money_to_buy_goods;
//                }
//            });
//
//    public static Action<Firms> production =
//            Action.create(Firms.class, Firms -> {
//
//                Firms.wealth -= Firms.production_cost;
//                    });
//
//    public static Action<Firms> selling_products =
//            Action.create(Firms.class, Firms -> {
//
//                Firms.wealth += Firms.total_money_of_products_sold;
//            });
//
//    public static Action<Firms> receiveMoneyForGoods =
//            Action.create(Firms.class, Firms -> {
//
//                Firms.getMessagesOfType(Messages.PaymentMessage.class).forEach(msg -> {
//                    Firms.wealth += Firms.money_to_buy_goods;
//                });
//            });


    @Variable
    public int vacancies;

    @Variable
    public int sector;

    @Override
    public void init() {
        vacancies = getPrng().getNextInt(50);
        Random rand = new Random();
        sector = rand.nextInt(3);
    }

//
//    private Boolean isEmployable(){
//        return Workers.worker_sector == sector;
//    }
//////
//    void hire(){
//        if (Workers.isEmployed = false && isEmployable() && vacancies > 0) {
//            vacancies -= 1;
//            Workers.isEmployed = true;
//        }
//    }

//    void hire() {
//        int worker_sector = getMessageOfType(Messages.JobApplication.class).getBody();
//        if (worker_sector == sector && vacancies > 0){
//            vacancies -=1;
//        }
//    }
    private static Action<Firms> action(SerializableConsumer<Firms> consumer) {
        return Action.create(Firms.class, consumer);
    }
    public static Action<Firms> hire() {
        return action(
                firm -> {
                    int worker_sector = firm.get_worker_sector();
                    if (worker_sector == firm.sector && firm.vacancies > 0){
                        firm.vacancies -=1;
                    }
                }
        );
    }

    private int get_worker_sector(){
        return getMessageOfType(Messages.JobApplication.class).getBody();
    }

    }

