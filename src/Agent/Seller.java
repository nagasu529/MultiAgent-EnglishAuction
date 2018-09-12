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
/**
 *
 * @author chiewchk
 */
public class Seller extends Agent{

    private SellerGUI myGui;
    Crop calCrops = new Crop();

    //set agent status after calculating water reduction on farm.
    String agentStatus;
    double volumeToSell;
    double volumeToBuy;
    double sellingPrice;
    double buyingPrice;
    DecimalFormat df = new DecimalFormat("#.##");

    //The list of known water selling agent
    private AID[] sellerAgent;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0, 0.0, "avalable",0.0, 0.0);

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup(){
        System.out.println(getAID()+" is ready");

        //Creating catalogue and running GUI
        myGui = new SellerGUI(this);
        myGui.show();

        //Start agent and register all service.
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        farmerInfo.agentType = "Seller";
        sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        farmerInfo.maxPricePerMM = 300;
        farmerInfo.incresingRate = 10;
        farmerInfo.waterVolumn = 100;


        myGui.displayUI("Hello "+ getAID().getName() + "\n" + "Stage is " + sd.getType() + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                    /*
                     ** Selling water process
                     */
                    addBehaviour(new OfferRequestsServer());
                    // Add the behaviour serving purchase orders from buyer agents
                    addBehaviour(new PurchaseOrdersServer());
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
                    resultCal.append(st.cropName + " " + st.cropStage +
                            " " + st.droubhtSensitivity + " " + df.format(st.dsValue) + " " + df.format(st.stValue) + " " + df.format(st.cvValue) +
                            " " + df.format(st.literPerSecHec) + " " + df.format(st.waterReq) + " " + df.format(st.soilWaterContainValue) + " " + df.format(st.waterReqWithSoil) +
                            " " + df.format(st.cropCoefficient) + " " + df.format(st.waterReduction) + " " + df.format(st.productValueLost) + "\n");
                }
                //System.out.println("Actual reduction is: " + actualReduction);
                resultCal.append("Actual reduction is: " + actualReduction + "\n");
                resultCal.append("\n");
                /***
                if (actualReduction >= (calCrops.totalWaterReq*totalWaterReductionPctg)) {
                    farmerInfo.agentType = "seller";
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
                ***/
            }
        } );
    }

    /*
     *	OfferRequestsSerer
     *	This behaviour is used b Seller mechanism for water buying request form other agent.
     *	If the requested water capacity and price match with buyer, the seller replies with a PROPOSE message specifying the price.
     *	Otherwise a REFUSE message is send back.
     * and PurchaseOrderServer is required by agent when the agent status is "Seller"
     */
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {

            //Register service to DFDAgent

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            String log = new String();
            if (msg != null) {
                // CFP Message received. Process it
                ACLMessage reply = msg.createReply();
                if (farmerInfo.sellingStatus== "avalable") {
                    // The requested water is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(farmerInfo.waterVolumn));
                } else {
                    // The requested water is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("do not water for sale");
                }
                myAgent.send(reply);
                myGui.displayUI(log + "\n");
            }else {
                block();
            }
        }
    }
    /*
     * 	PurchaseOrderServer
     * 	This behaviour is used by Seller agent to serve incoming offer acceptances (purchase orders) from buyer.
     * 	The seller agent will remove selling list and replies with an INFORM message to notify the buyer that purchase has been
     * 	successfully complete.
     */

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
        double incresingRate;
        double minPricePerMM;
        double maxPricePerMM;

        agentInfo(String farmerName, String agentType, double waterVolumn, double pricePerMM, String sellingStatus, double incresingRate, double minPricePerMM, double maxPricePerMM){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.pricePerMM = pricePerMM;
            this.sellingStatus = sellingStatus;
            this.incresingRate = incresingRate;
            this.minPricePerMM = minPricePerMM;
            this.maxPricePerMM = maxPricePerMM;
        }
    }
}
