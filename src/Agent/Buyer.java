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

public class Buyer extends Agent {
    private SellerGUI myGui;
    Crop calculationCrops = new Crop();
    DecimalFormat df = new DecimalFormat("#.##");
    agentInfo buyer = new agentInfo(getAID().getName(),"seller", 6000, 30, );

    //List of the sellers
    private AID[] sellerAgents;
    protected void setup(){
        System.out.println(getAID().getName()+ " is ready");

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
