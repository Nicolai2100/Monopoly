package dk.dtu.compute.se.pisd.monopoly.mini.view;

import dk.dtu.compute.se.pisd.monopoly.mini.model.*;
import dk.dtu.compute.se.pisd.monopoly.mini.model.properties.RealEstate;
import dk.dtu.compute.se.pisd.monopoly.mini.model.properties.Utility;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.util.Map;

import static dk.dtu.compute.se.pisd.monopoly.mini.model.ColorGroup.lightblue;

/**
 * @Malte og Nicolai L
 */
public class PlayerPanel extends JFrame {
    private Game game;
    private Player player;
    private JPanel mainPanel;
    private Dimension dimension;
    private Map<ColorGroup, JPanel> hasherMalte; //TODO lol

    public PlayerPanel(Game game, Player player) {
        this.game = game;
        this.player = player;
        dimension = new Dimension(72, 100);

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        this.setContentPane(mainPanel);
        this.setLocation(710, game.getPlayers().indexOf(player) * 120);
        this.setSize(800, 120);
        this.validate();
        this.setVisible(true);
        update();

//Måske noget med en fængselsstatus

    }

    public void update() {
        mainPanel.removeAll();

        JPanel playerPanel = new JPanel();
        playerPanel.setBackground(player.getColor());
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
        playerPanel.setBorder(new EtchedBorder());
        JLabel nameLabel = new JLabel(player.getName());
        playerPanel.add(nameLabel);
        JLabel balanceLabel = new JLabel("" + player.getBalance());
        playerPanel.add(balanceLabel);
        playerPanel.setMaximumSize(dimension);
        playerPanel.setPreferredSize(dimension);
        mainPanel.add(playerPanel);

        for (ColorGroup color : ColorGroup.values()) {
            JPanel jPanel = panelMaker(color);
        }

        for (Property property : player.getOwnedProperties()) {

        }

        this.revalidate();
        this.repaint();
    }

    public JPanel panelMaker(ColorGroup color) {

        JPanel colorGroupPanel = new JPanel();
        colorGroupPanel.setBackground(ColorGroup.colorGroupTransformer(color));
        colorGroupPanel.setLayout(new BoxLayout(colorGroupPanel, BoxLayout.Y_AXIS));
        colorGroupPanel.setBorder(new EtchedBorder());
        colorGroupPanel.setMaximumSize(dimension);
        colorGroupPanel.setPreferredSize(dimension);
        colorGroupPanel.setVisible(true);
        mainPanel.add(colorGroupPanel);

        return colorGroupPanel;
    }

    public void labelMaker(JPanel jPanel, String name) {
        JLabel jLabel = new JLabel(name);
        jPanel.add(jLabel);
    }
    /*Tilføj en metode update() til klassen, som først sletter den gamle inhold fra panelet
    og så kreirer nogle paneler på framet, som viser de forskellige informationer (se skematiske
    tegninger fra forelæsing 7).

Når I er lidt længere i opgaven, så kan I prøve at eksperimentere
 lidt med layoutet, farverne, og randen (border) for at finde ud af hvilken
  effekt de forskellige indstillinger har (see Java tutorials om layouts og borders).

*/

}
