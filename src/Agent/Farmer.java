package Agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import Agent.Crop.cropType;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author chiewchk
 */
public class Farmer extends Agent{

    //The list of farmer who are seller (maps the water volumn to its based price)
    private Hashtable waterCapacity;
    private FarmerGUI myGui;
    Crop calCrops = new Crop();

    DecimalFormat df = new DecimalFormat("#.##");

    //The list of known water selling agent
    private AID[] bidderAgent;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0, 0.0, "avalable", 0.0, 0.0, 0.0, 0.0);

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup(){
        System.out.println(getAID()+" is ready");

        //Creating catalogue and running GUI
        myGui = new FarmerGUI(this);
        myGui.show();
        //Start agent

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        farmerInfo.agentType = "Farmer";
        ServiceDescription sd = new ServiceDescription();
        sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myGui.displayUI("Hello "+ farmerInfo.farmerName + "\n" + "Stage is " + farmerInfo.agentType + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 100000){
            protected void onTick() {

                myGui.displayUI("Agent status is " + farmerInfo.agentType + "\n");
                if (farmerInfo.agentType=="owner"||farmerInfo.agentType=="Farmer-owner") {
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-owner";
                    sd.setType(farmerInfo.agentType);
                    sd.setName(getAID().getName());
                    farmerInfo.farmerName = getAID().getName();
                    farmerInfo.pricePerMM = 0.5;
                    farmerInfo.minPricePerMM = farmerInfo.pricePerMM;

                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("Volumn to sell: " + farmerInfo.waterVolumn + "\n");
                    myGui.displayUI("Selling price: " + farmerInfo.pricePerMM + "\n");
                    myGui.displayUI("Selling status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("Maximum bidding: " + farmerInfo.maxPricePerMM + "\n");
                    myGui.displayUI("Providing price" + "\n");
                    myGui.displayUI("\n");

                    /*
                     ** Selling water process
                     */

                    //update bidder list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found acutioneer agents:");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            System.out.println(bidderAgent[i].getName());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    addBehaviour(new RequestPerformer());

                    // Add the behaviour serving purchase orders from buyer agents
                    addBehaviour(new PurchaseOrdersServer());

                } else if(farmerInfo.agentType=="auctioneer"||farmerInfo.agentType=="Farmer-auctioneer"){
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-auctioneer";
                    sd.setType(farmerInfo.agentType);
                    sd.setName(getAID().getName());
                    farmerInfo.farmerName = getAID().getName();

                    //Bidding rate, MinPrice, MaxPrice setting
                    Auction incRate = new Auction();
                    farmerInfo.minPricePerMM = 1;
                    farmerInfo.maxPricePerMM = 10;
                    farmerInfo.waterVolumn = 4288.6134838;
                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("The target volume for buying : " + df.format(farmerInfo.waterVolumn) + "\n");
                    myGui.displayUI("Bidding price: " + df.format(farmerInfo.pricePerMM) + "\n");
                    myGui.displayUI("Bidding status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("\n");

                    /*
                     ** Bidding water process
                     */
                    //Add the behaviour serving queries from Water provider about current price.
                    addBehaviour(new OfferRequestsServer());

                    //Add the behaviour serving purhase orders from water provider agent.
                    addBehaviour(new PurchaseOrdersServer());
                }
            }
        } );
    }

    //Update input data from GUI which include water allocation on single farm.
    public void farmerInput(final String filenameGlob, final Double actualRate, final int etSeason) {
        StringBuilder resultCal = new StringBuilder();

        addBehaviour(new OneShotBehaviour() {
            public void action() {

                //Input parameters from GUI
                calCrops.readText(filenameGlob);
                double totalWaterReductionPctg = actualRate/100;
                //Choosing ET0 from database.
                switch(etSeason){
                    case 0:
                        calCrops.ET0Spring();

                        break;
                    case 1:
                        calCrops.ET0Summer();

                        break;
                    case 2:
                        calCrops.ET0Autumn();

                        break;
                    default:
                        calCrops.ET0Winter();

                }
                calCrops.ET = calCrops.avgET0;
                calCrops.farmFactorValues();
                double actualReduction = calCrops.calcWaterReduction(totalWaterReductionPctg);
                resultCal.append("\n");
                resultCal.append("Water reduction result:\n");
                resultCal.append("\n");
                resultCal.append("Actual reducion is:" + actualReduction + "\n");
                //myGui.displayUI(xx.toString());

                //Result calculation
                Iterator itrR=calCrops.resultList.iterator();
                while (itrR.hasNext()) {
                    cropType st = (cropType)itrR.next();
                    /*System.out.println(st.cropName + " " + st.cropStage +
                        " " + st.droubhtSensitivity + " " + st.dsValue + " " + st.stValue + " " + st.cvValue +
                        " " + st.literPerSecHec + " " + st.waterReq + " " + st.cropCoefficient + " " + st.waterReduction);*/
                    resultCal.append(st.cropName + " " + st.cropStage +
                            " " + st.droubhtSensitivity + " " + df.format(st.dsValue) + " " + df.format(st.stValue) + " " + df.format(st.cvValue) +
                            " " + df.format(st.literPerSecHec) + " " + df.format(st.waterReq) + " " + df.format(st.soilWaterContainValue) + " " + df.format(st.waterReqWithSoil) +
                            " " + df.format(st.cropCoefficient) + " " + df.format(st.waterReduction) + " " + df.format(st.productValueLost) + "\n");
                }
                //System.out.println("Actual reduction is: " + actualReduction);
                resultCal.append("Actual reduction is: " + actualReduction + "\n");
                resultCal.append("\n");

                if (actualReduction >= (calCrops.totalWaterReq*totalWaterReductionPctg)) {
                    farmerInfo.agentType = "owner";
                    farmerInfo.waterVolumn = actualReduction;
                    myGui.displayUI(resultCal.toString());
                    //Clean parameter
                    calCrops.resultList.clear();
                    calCrops.calList.clear();
                    calCrops.cropT.clear();
                    calCrops.cv.clear();
                    calCrops.ds.clear();
                    calCrops.order.clear();
                    calCrops.st.clear();
                }
            }
        } );
    }

    /*
     *	OfferRequestServer
     *	This behaviour is used b Seller mechanism for water buying request form other agent.
     *	If the requested water capacity and price match with buyer, the seller replies with a PROPOSE message specifying the price.
     *	Otherwise a REFUSE message is send back.
     * and PurchaseOrderServer is required by agent when the agent status is "Seller"
     */
    private class OfferRequestsServer extends CyclicBehaviour {
        Auction auctRules = new Auction();
        public void action() {

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            String log = new String();
            //CFP Message received. Process it.
            if(msg != null){
                String currentPriceAndBidding = msg.getContent();
                ACLMessage reply = msg.createReply();

                //Current price Per MM. and the number of volumn to sell.
                for (String retval : currentPriceAndBidding.split("-")){
                    farmerInfo.waterVolumn = Double.parseDouble(retval);
                    farmerInfo.currentPricePerMM = Double.parseDouble(retval);
                }
                //English Auction Process.
                if (farmerInfo.currentPricePerMM < farmerInfo.maxPricePerMM) {
                    farmerInfo.bidedPrice = auctRules.changedPriceRate("inc",10,farmerInfo.pricePerMM);
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(farmerInfo.waterVolumn-farmerInfo.bidedPrice));
                    myAgent.send(reply);
                    myGui.displayUI(log + "\n");
                }else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent(getAID().getName() + " is surrender");
                }
            }else {
                block();
            }
        }
    }

    /*
     * 	Request performer
     *
     * 	This behaviour is used by buyer mechanism to request seller agents for water pricing ana selling capacity.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestBidder; // The agent who provides the best offer
        private double bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        int numBidderReply = 0;     //Counting a number of bidder and finishing auvtion after bidder is only one propose message.
        double waterVolFromBidder;
        double biddedPriceFromBidder;

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        cfp.addReceiver(bidderAgent[i]);
                    }
                    //String arr = Double.toString(farmerInfo.waterVolumn)+"-"+Double.toString(farmerInfo.pricePerMM);
                    cfp.setContent(String.valueOf(Double.toString(farmerInfo.waterVolumn)+"-"+Double.toString(farmerInfo.pricePerMM)));
                    System.out.println(farmerInfo.waterVolumn);
                    System.out.println(farmerInfo.pricePerMM);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    System.out.println(step);
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents

                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //Count number of bidder that is propose message for water price bidding.
                            numBidderReply++;

                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            for(String retval: biddedFromAcutioneer.split("-")){
                                waterVolFromBidder = Double.parseDouble(retval);
                                biddedPriceFromBidder = Double.parseDouble(retval);
                            }

                            if (bestBidder == null || biddedPriceFromBidder < bestPrice) {
                                // This is the best offer at present
                                bestPrice = biddedPriceFromBidder;
                                bestBidder = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        System.out.println("The number of current bidding is " + numBidderReply);
                        System.out.println("Best price is from " + bestBidder);
                        System.out.println("Price : " + bestPrice);
                        farmerInfo.currentPricePerMM = bestPrice;

                        if (repliesCnt >= bidderAgent.length-1) {
                            // We received all replies

                            step = 2;
                            System.out.println(step);
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    if(numBidderReply==1){
                        step = 3;
                        System.out.println(step);
                    }else {
                        step = 0;
                        System.out.println(step);
                    }
                    break;
                case 3:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestBidder);
                    order.setContent(String.valueOf(farmerInfo.pricePerMM));
                    order.setConversationId("bidding");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    step = 4;
                    System.out.println(step);
                    break;
                case 4:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+bestPrice);
                            myGui.displayUI(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName().toString());
                            myGui.displayUI("Price = " + bestPrice);
                            doSuspend();
                            //myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold.");
                            myGui.displayUI("Attempt failed: requested water volumn already sold.");
                        }

                        step = 5;
                        System.out.println(step);
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestBidder == null) {
                //System.out.println("Attempt failed: "+volumeToBuy+" not available for sale");
                myGui.displayUI("Attempt failed: do not have seller now".toString());
            }
            return ((step == 2 && bestBidder == null) || step == 5);
        }
    }

    /*
     * 	PurchaseOrderServer
     * 	This behaviour is used by Seller agent to serve incoming offer acceptances (purchase orders) from buyer.
     * 	The seller agent will remove selling list and replies with an INFORM message to notify the buyer that purchase has been
     * 	successfully complete.
     */

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                myGui.displayUI(msg.toString());
                System.out.println(farmerInfo.sellingStatus);
                reply.setPerformative(ACLMessage.INFORM);
                if (farmerInfo.sellingStatus=="avalable") {
                    farmerInfo.sellingStatus = "sold";
                    //System.out.println(getAID().getName()+" sold water to agent "+msg.getSender().getName());
                    myGui.displayUI(getAID().getLocalName()+" sold water to agent "+msg.getSender().getLocalName());
                    //myGui.displayUI(farmerInfo.sellingStatus.toString());
                    //System.out.println(farmerInfo.sellingStatus);
                    doSuspend();
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available for sale");
                    myGui.displayUI("not avalable to sell");
                }

            }else {
                block();
            }
        }
    }

    public void updateCatalogue(final String agentName, final String agentType, final double waterVolumn, final double priceForSell){
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                //farmerInfo.
                //agentInfo agentInfo = new agentInfo(agentName, agentType, waterVolumn, priceForSell);
                //System.out.println(agentName+" need to sell water to others. The water volumn is = "+ volumeToSell);
                //System.out.println(agentInfo.agentType);
                //System.out.println(agentInfo.farmerName);
            }
        });
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double waterVolumn;
        double pricePerMM;
        String sellingStatus;
        double minPricePerMM;
        double maxPricePerMM;
        double currentPricePerMM;
        double bidedPrice;

        agentInfo(String farmerName, String agentType, double waterVolumn, double pricePerMM, String sellingStatus, double minPricePerMM, double maxPricePerMM, double currentPricePerMM, double biddedPrice){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.pricePerMM = pricePerMM;
            this.sellingStatus = sellingStatus;
            this.minPricePerMM = minPricePerMM;
            this.maxPricePerMM = maxPricePerMM;
            this.currentPricePerMM = currentPricePerMM;
            this.bidedPrice = biddedPrice;
        }
    }
}
