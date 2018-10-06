package Agent;

public class Auction {

    double pricePerMM;
    double maxSellingPrice;
    double minSelleingPrice;
    double increseRateAcution;


    public double englishAuction(double bidderPrice, double acutioneerPrice, double chageRatePct, double pricePerMM){
        changedPriceRate(chageRatePct, pricePerMM);
        if (acutioneerPrice < bidderPrice){
            double tempPrice = bidderPrice * increseRateAcution;
            return tempPrice;

        }else {
            return acutioneerPrice;
        }
    }

    public double dutchAcution(double bidderPrice, double auctioneerPrice, double chageRatePct, double pricePerMM){
        changedPriceRate(chageRatePct, pricePerMM);
        if (bidderPrice < auctioneerPrice){
            double temPrice = auctioneerPrice * increseRateAcution;
            return temPrice;
        }else {
             return auctioneerPrice;
        }

    }

    public double changedPriceRate(double changePerct, double pricePerMM){
        double temRriceRate = (changePerct/100)*pricePerMM;
        return temRriceRate;

    }

}
