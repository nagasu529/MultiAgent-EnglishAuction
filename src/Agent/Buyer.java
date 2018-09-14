package Agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.text.DecimalFormat;
import java.util.Iterator;

public class Buyer extends Agent {
    private SellerGUI myGui;
    Crop calculationCrops = new Crop();
    DecimalFormat df = new DecimalFormat("#.##");
    agentInfo buyer = new agentInfo(getAID().getName(),"seller", 6000, 30, "avalable");

    //List of the sellers
    private AID[] sellerAgents;
    protected void setup(){
        //Start agent and register all service.
        System.out.println(getAID().getName()+ " is ready");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        buyer.agentType = "Buyer";
        sd.setType(buyer.agentType);
        sd.setName(getAID().getName());
        buyer.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }

        myGui.displayUI("The agent stage is " + sd.getType() + "\n");

    }

    public void farmerInput(final String filenameGlob, final Double actualRate, final int etSeason) {
        StringBuilder resultCal = new StringBuilder();

        addBehaviour(new OneShotBehaviour() {
            public void action() {

                //Input parameters from GUI
                calculationCrops.readText(filenameGlob);
                double totalWaterReductionPctg = actualRate/100;
                //Choosing ET0 from database.
                switch(etSeason){
                    case 0:
                        calculationCrops.ET0Spring();

                        break;
                    case 1:
                        calculationCrops.ET0Summer();

                        break;
                    case 2:
                        calculationCrops.ET0Autumn();

                        break;
                    default:
                        calculationCrops.ET0Winter();

                }
                calculationCrops.ET = calculationCrops.avgET0;
                calculationCrops.farmFactorValues();
                double actualReduction = calculationCrops.calcWaterReduction(totalWaterReductionPctg);
                resultCal.append("\n");
                resultCal.append("Water reduction result:\n");
                resultCal.append("\n");
                resultCal.append("Actual reducion is:" + actualReduction + "\n");
                //myGui.displayUI(xx.toString());

                //Result calculation
                Iterator itrR=calculationCrops.resultList.iterator();
                while (itrR.hasNext()) {
                    Agent.Crop.cropType st = (Agent.Crop.cropType)itrR.next();
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

                if (actualReduction >= (calculationCrops.totalWaterReq*totalWaterReductionPctg)) {
                    buyer.agentType = "seller";
                    buyer.waterVolumn = actualReduction;
                    myGui.displayUI(resultCal.toString());
                    //Clean parameter
                    calculationCrops.resultList.clear();
                    calculationCrops.calList.clear();
                    calculationCrops.cropT.clear();
                    calculationCrops.cv.clear();
                    calculationCrops.ds.clear();
                    calculationCrops.order.clear();
                    calculationCrops.st.clear();
                }
            }
        } );
    }

    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= sellerAgents.length) {
                            // We received all replies
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+bestPrice);
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested book already sold.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: "+targetBookTitle+" not available for sale");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }

    //Agent Information
    public class agentInfo{
        String farmerName;
        String agentType;
        double waterVolumn;
        double pricePerMM;
        String status;

        agentInfo(String farmerName, String agentType, double waterVolumn, double pricePerMM, String status){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.pricePerMM = pricePerMM;
            this.status = status;
        }
    }

}
