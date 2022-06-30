package macro_financial_framework;

import simudyne.core.abm.Agent;
import simudyne.core.abm.Action;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GoodsMarket extends Agent<MacroFinancialModel.Globals> {

    public int goodTraded;
    public boolean competitive;

    public List<FirmSupplyInformation> firmsSupplyingGoods;
    public List<HouseholdDemandInformation> householdsDemandingGoods;

    public static Action<GoodsMarket> matchSupplyAndDemand() {
        return Action.create(GoodsMarket.class, goodMarket -> {

            // storing the supply for this good
            if (goodMarket.hasMessageOfType(Messages.FirmSupply.class)) {
                goodMarket.getMessagesOfType(Messages.FirmSupply.class).forEach(firmSupply -> {
                    goodMarket.firmsSupplyingGoods.add(new FirmSupplyInformation(firmSupply.getSender(), firmSupply.output, firmSupply.price));
                });
            }

            // storing the demand for this good
            if (goodMarket.hasMessageOfType(Messages.HouseholdDemand.class)) {
                goodMarket.getMessagesOfType(Messages.HouseholdDemand.class).forEach(householdDemand -> {
                    goodMarket.householdsDemandingGoods.add(new HouseholdDemandInformation(householdDemand.getSender(), householdDemand.consumptionBudget));
                });
            }

            // households with the highest consumption budget will have higher priority
            goodMarket.householdsDemandingGoods.sort(Comparator.comparing(household -> household.consumptionBudget));


            // iterate over all the households, starting from the ones with most demand and match them to a firm that has supply and the cheapest price
            goodMarket.householdsDemandingGoods.forEach(household -> {

                // sorts the price of the goods from highest to lowest
                goodMarket.firmsSupplyingGoods.sort(Comparator.comparing(firm -> firm.price));

                double demandFromHousehold = household.consumptionBudget;

                while (demandFromHousehold > 0){

                    // gets the last firm in the list, i.e. the one with the cheapest price
                    Optional<FirmSupplyInformation> firmToPurchaseFrom = goodMarket.firmsSupplyingGoods.stream().reduce((first, second) -> second);

                    if (firmToPurchaseFrom.isPresent()){
                        double priceOfGood = firmToPurchaseFrom.get().price;
                        long quantityAvailable = firmToPurchaseFrom.get().output;
                        int quantityDemanded = (int) Math.floor(demandFromHousehold / priceOfGood);

                        if (quantityDemanded == 0) {
                            // logic: this is the cheapest product it can find and if it can't afford that it won't be able to afford that it won't be able to afford anything
                            break;
                        }

                        else if (quantityDemanded >= quantityAvailable){
                            // the house is demanding more than the supply of the firm
                            // it buys everything from the firm and looks for the next cheapest one to complete the purchase
                            goodMarket.send(Messages.HouseholdWantsToPurchase.class, message -> {
                                message.demand = quantityDemanded;
                                message.bought = quantityAvailable;
                            }).to(firmToPurchaseFrom.get().ID);

                            // sends a message to the household of how much its spent
                            goodMarket.send(Messages.PurchaseCompleted.class, spentMessage -> {
                                spentMessage.spent = quantityAvailable;
                            }).to(household.ID);

                            // update the consumption budget of the household
                            demandFromHousehold = demandFromHousehold - (quantityAvailable * priceOfGood);

                            // this firm no longer has any available goods -> it's removed from the list of firms
                            goodMarket.firmsSupplyingGoods.remove(firmToPurchaseFrom);
                        }

                        else if (quantityDemanded < quantityAvailable){
                            goodMarket.send(Messages.HouseholdWantsToPurchase.class, m -> {
                                m.demand = quantityDemanded;
                                m.bought = quantityDemanded;
                            });

                            // sends a message to the household of how much its spent
                            goodMarket.send(Messages.PurchaseCompleted.class, spentMessage -> {
                                spentMessage.spent = quantityDemanded;
                            }).to(household.ID);

                            // update the consumption budget of the household
                            demandFromHousehold = demandFromHousehold - (quantityDemanded * priceOfGood);

                            // remove the firm and add it again with the updated quantity available
                            // TODO: find a better way of doing this
                            goodMarket.firmsSupplyingGoods.remove(firmToPurchaseFrom);
                            goodMarket.firmsSupplyingGoods.add(new FirmSupplyInformation(firmToPurchaseFrom.get().ID, firmToPurchaseFrom.get().output, firmToPurchaseFrom.get().price));

                        }
                    } else {
                        // when there are no more firms to purchase from
                        break;
                    }

                }
            });
        });
    }

}

