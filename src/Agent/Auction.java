package Agent;

public class Auction {

    public double changedPriceRate(double changePerct, double pricePerMM){
        double temRriceRate = (pricePerMM + (changePerct/100)*pricePerMM);
        return temRriceRate;
    }

}
