package Agent;

public class Auction {

    double pricePerMM;
    double maxSellingPrice;
    double minSelleingPrice;
    double increseRateAcution;


    public double englishAuction(double bidderPrice, double acutioneerPrice){
        if (acutioneerPrice < bidderPrice){
            double tempPrice = bidderPrice * increseRateAcution;
            return tempPrice;

        }else {
            return acutioneerPrice;
        }
    }

    public void JapaneaseAuction(){

    }

    public void changedPriceRate(double changePerct){

    }

}
