package SimpleEconomyModel.agents;

import SimpleEconomyModel.utils.*;
import simudyne.core.abm.Agent;
import simudyne.core.abm.Action;

import java.util.*;

public class GoodsMarket extends Agent<Globals> {

    public int goodTraded;
    public List<FirmSupplyInformation> firmsSupplyingGoods;
    public List<HouseholdDemandInformation> householdsDemandingGoods;
    public List<FirmsIntermediateGoodDemand> firmsDemandingIntermediateGoods;

    public HashMap<Integer, Double> priceOfGoods;
    private double numerator; // this is the sum of the product of the price and output for each firm
    private double denominator; // this is the sum of the output of every firm

    public static Action<GoodsMarket> matchSupplyAndDemand() {
        return Action.create(GoodsMarket.class, goodMarket -> {

            goodMarket.firmsSupplyingGoods.clear();
            goodMarket.householdsDemandingGoods.clear();
            goodMarket.firmsDemandingIntermediateGoods.clear();

            // storing the demand of intermediate goods
            if (goodMarket.hasMessageOfType(Messages.PurchaseIntermediateGood.class)) {
                goodMarket.getMessagesOfType(Messages.PurchaseIntermediateGood.class).forEach(firmDemanding -> {
                    goodMarket.firmsDemandingIntermediateGoods.add(new FirmsIntermediateGoodDemand(firmDemanding.getSender(), firmDemanding.demand));
                });
            }

            // storing the supply for this good
            if (goodMarket.hasMessageOfType(Messages.FirmSupply.class)) {
                goodMarket.getMessagesOfType(Messages.FirmSupply.class).forEach(firmSupply -> {
//                    System.out.println("receiving a supply of " + firmSupply.output + " from firm " + firmSupply.getSender() + " in good market " + goodMarket.goodTraded);
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

            Collections.shuffle(goodMarket.firmsSupplyingGoods, goodMarket.getPrng().getRandom());
            Collections.shuffle(goodMarket.householdsDemandingGoods, goodMarket.getPrng().getRandom());
            Collections.shuffle(goodMarket.firmsDemandingIntermediateGoods, goodMarket.getPrng().getRandom());

            // (1) it first sells to the firms that are demanding goods for production

            // demand has to be sorted in descending order, giving priority to those that are demanding bigger quantities
            goodMarket.firmsDemandingIntermediateGoods.sort(Comparator.comparing(firmsDemand -> firmsDemand.quantityDemanded));
            Collections.reverse(goodMarket.firmsDemandingIntermediateGoods);

            goodMarket.firmsDemandingIntermediateGoods.forEach(firm -> {

                // firms will look for the lowest price of the good they are demanding -> sorted in descending order
                goodMarket.firmsSupplyingGoods.sort(Comparator.comparing(firmsSupply -> firmsSupply.price));

                int quantityDemanded = firm.quantityDemanded;
                while (quantityDemanded > 0) {
                    // optional firm to buy from, chooses the first one, that is the cheapest
                    Optional<FirmSupplyInformation> firmToBuyFrom = goodMarket.firmsSupplyingGoods.stream().findFirst();

                    // to ensure firm is not buying from itself
                    if (firmToBuyFrom.isPresent() && firmToBuyFrom.get().ID != firm.ID) {
                        long quantityAvailable = firmToBuyFrom.get().output;
                        double priceOfIntermediateGood = firmToBuyFrom.get().price;

                        // if the firm supplying goods has more goods available than what the firm wants to purchase
                        if (quantityDemanded <= quantityAvailable) {

                            // sends a message to the firm about the intermediate good its bought
                            int finalQuantityDemanded = quantityDemanded;
                            goodMarket.send(Messages.IntermediateGoodBought.class, purchaseInformation -> {
                                purchaseInformation.good = goodMarket.goodTraded;
                                purchaseInformation.quantity = finalQuantityDemanded;
                                purchaseInformation.spent = finalQuantityDemanded * priceOfIntermediateGood;
                            }).to(firm.ID);

                            goodMarket.send(Messages.HouseholdOrFirmWantsToPurchase.class, demandInformation -> {
                                demandInformation.demand = finalQuantityDemanded;
                                demandInformation.bought = finalQuantityDemanded;
                            }).to(firmToBuyFrom.get().ID);

                            // updates list of bought items and its price
                            goodMarket.priceOfGoods.put(finalQuantityDemanded, priceOfIntermediateGood);

                            // remove and add the firm with the update quantity available
                            goodMarket.firmsSupplyingGoods.remove(firmToBuyFrom.get());
                            quantityAvailable -= quantityDemanded;

                            if (quantityAvailable > 0) {
                                goodMarket.firmsSupplyingGoods.add(new FirmSupplyInformation(firmToBuyFrom.get().ID, quantityAvailable, firmToBuyFrom.get().price));
                            }
                            quantityDemanded = 0;
                        }

                        // if the quantity demanded is more than the quantity available
                        // the firm will purchase all of it and move to the next firm supplying intermediate goods
                        else if (quantityDemanded > quantityAvailable) {
                            long finalQuantityAvailable = quantityAvailable;
                            goodMarket.send(Messages.IntermediateGoodBought.class, purchaseInformation -> {
                                purchaseInformation.good = goodMarket.goodTraded;
                                purchaseInformation.spent = finalQuantityAvailable * priceOfIntermediateGood;
                                purchaseInformation.quantity = (int) finalQuantityAvailable;
                            }).to(firm.ID);

                            int finalQuantityDemanded1 = quantityDemanded;
                            long finalQuantityAvailable1 = quantityAvailable;
                            goodMarket.send(Messages.DemandOfIntermediateGood.class, demandInformation -> {
                                demandInformation.demand = finalQuantityDemanded1;
                                demandInformation.bought = (int) finalQuantityAvailable1;
                            }).to(firmToBuyFrom.get().ID);

                            // updates list of bought items and its price
                            goodMarket.priceOfGoods.put((int) finalQuantityAvailable1, priceOfIntermediateGood);

                            // update the demanded quantity
                            quantityDemanded -= quantityAvailable;

                            // remove the firm from the list of firms supplying goods as it no longer has stock
                            goodMarket.firmsSupplyingGoods.remove(firmToBuyFrom.get());
                        }
                    } else {
                        // when there are no more firms to purchase from
                        break;
                    }
                }
            });

            // (2) it will then sell to the households demanding goods

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

//                        System.out.println("Household " + household.ID + " wants to purchase " + quantityDemanded);
//                        System.out.println("Firm " + firmToPurchaseFrom.get().ID + " is supplying " + quantityAvailable);

                        if (quantityDemanded == 0) {
                            // logic: this is the cheapest product it can find and if it can't afford that it won't be able to afford that it won't be able to afford anything
                            break;
                        } else if (quantityDemanded >= quantityAvailable) {
                            // the house is demanding more than the supply of the firm
                            // it buys everything from the firm and looks for the next cheapest one to complete the purchase
                            long finalQuantityAvailable = quantityAvailable;
                            goodMarket.send(Messages.HouseholdOrFirmWantsToPurchase.class, message -> {
//                                System.out.println("sending a message to firm " + firmToPurchaseFrom.get().ID + ". Bought: " + finalQuantityAvailable);
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

//                            System.out.println("Firm " + firmToPurchaseFrom.get().ID + " is sold out.");

                        } else if (quantityDemanded < quantityAvailable) {
                            goodMarket.send(Messages.HouseholdOrFirmWantsToPurchase.class, m -> {
                                m.demand = quantityDemanded;
                                m.bought = quantityDemanded;
                            }).to(firmToPurchaseFrom.get().ID);

                            // sends a message to the household of how much its spent
                            goodMarket.send(Messages.PurchaseCompleted.class, spentMessage -> {
                                spentMessage.spent = quantityDemanded * priceOfGood;
                            }).to(household.ID);

                            // updates list of bought items and its price
                            goodMarket.priceOfGoods.put((int) quantityDemanded, priceOfGood);

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
//                        System.out.println("no more firm to buy from, good traded: " + goodMarket.goodTraded);
                        // when there are no more firms to purchase from
                        break;
                    }
                }
            });
        });
    }
}

//    public static Action<GoodsMarket> CalculateAveragePrice() {
//        return Action.create(GoodsMarket.class, goodsMarket -> {
//            goodsMarket.priceOfGoods.forEach((quantity, price) -> {
//               goodsMarket.getLinks(Links.GoodsMarketToEconomy.class).send(Messages.SoldGoodsAndPrices.class, (msg, link) -> {
//                  msg.price = price;
//                  msg.quantitySold = quantity;
//               });
//            });
//        });
//    }
//}

