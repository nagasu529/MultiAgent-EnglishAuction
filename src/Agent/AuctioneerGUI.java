package Agent;

import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class AuctioneerGUI extends JFrame {
    private Auctioneer auctioneerAgent;

    //Creating setter and getter for passing parameters.
    public static Double sMinPrice, sMaxPrice, sIncreasePriceRate, sVolumnToBuy;

    public void setMinPrice(Double minPrice){
        sMinPrice = minPrice;
    }
    public static Double getMinPrice(){
        return sMinPrice;
    }

    public void setMaxPrice(Double maxPrice){
        sMaxPrice = maxPrice;
    }
    public static Double getMaxPrice(){
        return sMaxPrice;
    }

    public void setIncreasePriceRate(Double increasePriceRate){
        sIncreasePriceRate = increasePriceRate;
    }
    public static Double getIncreasePriceRate(){
        return sIncreasePriceRate;
    }

    public void setVolumeToBuy(Double volumeToBuy){
        sVolumnToBuy = volumeToBuy;
    }

    public static Double getVolumnToBuy(){
        return sVolumnToBuy;
    }

    private JTextField minPriceField, maxPriceField, increasePriceRate, volumeToBuy;
    private JTextArea log;

    AuctioneerGUI(Auctioneer a) {
        super(a.getLocalName().concat(" Bidding information"));

        auctioneerAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("Minimum price:"));
        minPriceField = new JTextField(5);
        p.add(minPriceField);

        p.add(new JLabel("Maximum price:"));
        maxPriceField = new JTextField(5);
        p.add(maxPriceField);

        p.add(new JLabel("Increasing rate (%)"));
        increasePriceRate = new JTextField(5);
        p.add(increasePriceRate);

        p.add(new JLabel("Volume to buy:"));
        volumeToBuy = new JTextField(5);
        p.add(volumeToBuy);


        getContentPane().add(p, BorderLayout.CENTER);

        //log area create
        log = new JTextArea(5,20);
        log.setEditable(false);
        getContentPane().add(log, BorderLayout.CENTER);
        log.setMargin(new Insets(5,5,50,5));
        JScrollPane logScrollPane = new JScrollPane(log);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Bid");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String minPirce = minPriceField.getText().trim();
                    setMinPrice(Double.parseDouble(minPirce));
                    String maxPrice = maxPriceField.getText().trim();
                    setMaxPrice(Double.parseDouble(maxPrice));
                    String increseRate = increasePriceRate.getText().trim();
                    setIncreasePriceRate(Double.parseDouble(increseRate));
                    String volume = volumeToBuy.getText().trim();
                    setVolumeToBuy(Double.parseDouble(volume));
                    auctioneerAgent.acutioneerInput(getMinPrice(), getMaxPrice(),getIncreasePriceRate(), getVolumnToBuy());
                    minPriceField.setText("");
                    maxPriceField.setText("");
                    increasePriceRate.setText("");
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(AuctioneerGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } );
        //p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                auctioneerAgent.doDelete();
            }
        } );

        setResizable(false);
    }

    public void show() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.show();
    }

    public void displayUI(String displayUI) {
        log.append(displayUI);
        log.setCaretPosition(log.getDocument().getLength());
    }
}
