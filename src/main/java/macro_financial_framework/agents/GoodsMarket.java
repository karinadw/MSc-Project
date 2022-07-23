package macro_financial_framework.agents;

import macro_financial_framework.*;
import macro_financial_framework.utils.*;
import simudyne.core.abm.Agent;
import simudyne.core.abm.Action;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GoodsMarket extends Agent<Globals> {

    public int goodTraded;
    public boolean competitive;

    public List<FirmSupplyInformation> firmsSupplyingGoods;
    public List<FirmSupplyInformation> firmsSupplyingIntermediateGoods;
    public List<HouseholdDemandInformation> householdsDemandingGoods;
    public List<FirmsIntermediateGoodDemand> firmsDemandingIntermediateGoods;

    public static Action<GoodsMarket> matchSupplyAndDemand() {
        return Action.create(GoodsMarket.class, goodMarket -> {

            goodMarket.firmsDemandingIntermediateGoods.clear();
            goodMarket.firmsSupplyingGoods.clear();
            // storing the supply for this good
            if (goodMarket.hasMessageOfType(Messages.FirmSupply.class)) {
                goodMarket.getMessagesOfType(Messages.FirmSupply.class).forEach(firmSupply -> {
                    if (firmSupply.output > 0) {
                        goodMarket.firmsSupplyingGoods.add(new FirmSupplyInformation(firmSupply.getSender(), firmSupply.output, firmSupply.price));
                    }
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

            // Comparator.comparing sorts it from low to high, so we reverse it to give bigger consumers priority
            Collections.reverse(goodMarket.householdsDemandingGoods);


            // iterate over all the households, starting from the ones with most demand and match them to a firm that has supply and the cheapest price
            goodMarket.householdsDemandingGoods.forEach(household -> {

                double demandFromHousehold = household.consumptionBudget;

                while (demandFromHousehold > 0) {

                    // sorts the price of the goods from lowest to highest -> consumers want to find the cheapest goods
                    goodMarket.firmsSupplyingGoods.sort(Comparator.comparing(firm -> firm.price));

                    // gets the last firm in the list, i.e. the one with the cheapest price
                    Optional<FirmSupplyInformation> firmToPurchaseFrom = goodMarket.firmsSupplyingGoods.stream().findFirst();

                    if (firmToPurchaseFrom.isPresent()) {
                        double priceOfGood = firmToPurchaseFrom.get().price;
                        long quantityAvailable = firmToPurchaseFrom.get().output;
                        int quantityDemanded = (int) Math.floor(demandFromHousehold / priceOfGood);

                        if (quantityDemanded == 0) {
                            // logic: this is the cheapest product it can find and if it can't afford that it won't be able to afford that it won't be able to afford anything
                            break;
                        } else if (quantityDemanded >= quantityAvailable) {
                            // the house is demanding more than the supply of the firm
                            // it buys everything from the firm and looks for the next cheapest one to complete the purchase
                            long finalQuantityAvailable = quantityAvailable;
                            goodMarket.send(Messages.HouseholdWantsToPurchase.class, message -> {
                                message.demand = quantityDemanded;
                                message.bought = finalQuantityAvailable;
                            }).to(firmToPurchaseFrom.get().ID);

                            // sends a message to the household of how much its spent
                            long finalQuantityAvailable1 = quantityAvailable;
                            goodMarket.send(Messages.PurchaseCompleted.class, spentMessage -> {
                                spentMessage.spent = finalQuantityAvailable1 * priceOfGood;
                            }).to(household.ID);

                            // update the consumption budget of the household
                            demandFromHousehold -= (quantityAvailable * priceOfGood);

                            // this firm no longer has any available goods -> it's removed from the list of firms
                            goodMarket.firmsSupplyingGoods.remove(firmToPurchaseFrom.get());

                        } else if (quantityDemanded < quantityAvailable) {
                            goodMarket.send(Messages.HouseholdWantsToPurchase.class, m -> {
                                m.demand = quantityDemanded;
                                m.bought = quantityDemanded;
                            }).to(household.ID);

                            // sends a message to the household of how much its spent
                            goodMarket.send(Messages.PurchaseCompleted.class, spentMessage -> {
                                spentMessage.spent = quantityDemanded;
                            }).to(household.ID);

                            // update the consumption budget of the household
                            demandFromHousehold = demandFromHousehold - (quantityDemanded * priceOfGood);
                            quantityAvailable -= quantityDemanded;

                            // remove the firm and add it again with the updated quantity available (which is previous quantity available minus the quantity demanded
                            goodMarket.firmsSupplyingGoods.remove(firmToPurchaseFrom.get());
                            if (quantityAvailable > 0) {
                                goodMarket.firmsSupplyingGoods.add(new FirmSupplyInformation(firmToPurchaseFrom.get().ID, quantityAvailable, firmToPurchaseFrom.get().price));
                            }
                        }
                    } else {
                        // when there are no more firms to purchase from
                        break;
                    }
                }
            });
        });
    }

    public static Action<GoodsMarket> MatchSupplyAndDemandIntermediateGoods() {
        return Action.create(GoodsMarket.class, intermediateGoodsMarket -> {

            intermediateGoodsMarket.firmsSupplyingIntermediateGoods.clear();
            intermediateGoodsMarket.firmsDemandingIntermediateGoods.clear();

            // store all the information of the firms that supply intermediate good
            if (intermediateGoodsMarket.hasMessageOfType(Messages.StockOfIntermediateGood.class)) {
                intermediateGoodsMarket.getMessagesOfType(Messages.StockOfIntermediateGood.class).forEach(msg -> {
                    if (msg.stock > 0) {
                        intermediateGoodsMarket.firmsSupplyingIntermediateGoods.add(new FirmSupplyInformation(msg.getSender(), msg.stock, msg.price));
                    }
                });
            }

            // stores the demand of the intermediate goods
            if (intermediateGoodsMarket.hasMessageOfType(Messages.PurchaseIntermediateGood.class)) {
                intermediateGoodsMarket.getMessagesOfType(Messages.PurchaseIntermediateGood.class).forEach(m -> {
                    if (m.demand > 0) {
                        intermediateGoodsMarket.firmsDemandingIntermediateGoods.add(new FirmsIntermediateGoodDemand(m.getSender(), m.demand));
                    }
                });
            }

            // demand has to be sorted in descending order, giving priority to those that are demanding bigger quantities
            intermediateGoodsMarket.firmsDemandingIntermediateGoods.sort(Comparator.comparing(firmsDemand -> firmsDemand.quantityDemanded));
            Collections.reverse(intermediateGoodsMarket.firmsDemandingIntermediateGoods);


            intermediateGoodsMarket.firmsDemandingIntermediateGoods.forEach(firm -> {

                // firms will look for the lowest price of the good they are demanding -> sorted in descending order
                intermediateGoodsMarket.firmsSupplyingIntermediateGoods.sort(Comparator.comparing(firmsSupply -> firmsSupply.price));
//
                int quantityDemanded = firm.quantityDemanded;
                while (quantityDemanded > 0) {
                    // optional firm to buy from, chooses the first one, that is the cheapest
                    Optional<FirmSupplyInformation> firmToBuyFrom = intermediateGoodsMarket.firmsSupplyingIntermediateGoods.stream().findFirst();

                    if (firmToBuyFrom.isPresent()) {
                        long quantityAvailable = firmToBuyFrom.get().output;
                        double priceOfIntermediateGood = firmToBuyFrom.get().price;

                        // if the firm supplying goods has more goods available than what the firm wants to purchase
                        if (quantityDemanded <= quantityAvailable) {
                            int finalQuantityDemanded = quantityDemanded;
                            intermediateGoodsMarket.send(Messages.IntermediateGoodBought.class, purchaseInformation -> {
                                purchaseInformation.quantity = finalQuantityDemanded;
                                purchaseInformation.spent = finalQuantityDemanded * priceOfIntermediateGood;
                            }).to(firm.ID);

                            intermediateGoodsMarket.send(Messages.DemandOfIntermediateGood.class, demandInformation -> {
                               demandInformation.demand = finalQuantityDemanded;
                               demandInformation.bought = finalQuantityDemanded;
                            }).to(firmToBuyFrom.get().ID);

                            // remove and add the firm with the update quantity available
                            intermediateGoodsMarket.firmsSupplyingIntermediateGoods.remove(firmToBuyFrom.get());
                            quantityAvailable -= quantityDemanded;

                            if (quantityAvailable > 0) {
                                intermediateGoodsMarket.firmsSupplyingIntermediateGoods.add(new FirmSupplyInformation(firmToBuyFrom.get().ID, quantityAvailable, firmToBuyFrom.get().price));
                            }
                            quantityDemanded = 0;
                        }

                        // if the quantity demanded is more than the quantity available
                        // the firm will purchase all of it and move to the next firm supplying intermediate goods
                        else if (quantityDemanded > quantityAvailable){
                            long finalQuantityAvailable = quantityAvailable;
                            intermediateGoodsMarket.send(Messages.IntermediateGoodBought.class, purchaseInformation -> {
                               purchaseInformation.spent = finalQuantityAvailable * priceOfIntermediateGood;
                               purchaseInformation.quantity = (int) finalQuantityAvailable;
                            }).to(firm.ID);

                            int finalQuantityDemanded1 = quantityDemanded;
                            long finalQuantityAvailable1 = quantityAvailable;
                            intermediateGoodsMarket.send(Messages.DemandOfIntermediateGood.class, demandInformation -> {
                               demandInformation.demand = finalQuantityDemanded1;
                               demandInformation.bought = (int) finalQuantityAvailable1;
                            }).to(firmToBuyFrom.get().ID);

                            // update the demanded quantity
                            quantityDemanded -= quantityAvailable;

                            // remove the firm from the list of firms supplying goods as it no longer has stock
                            intermediateGoodsMarket.firmsSupplyingIntermediateGoods.remove(firmToBuyFrom.get());
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

