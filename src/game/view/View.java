package game.view;

import designpattern.Observer;
import designpattern.Subject;
import game.model.Game;
import game.model.Player;
import game.model.Property;
import game.model.Space;
import game.model.properties.RealEstate;
import gui_fields.*;
import gui_fields.GUI_Car.Pattern;
import gui_fields.GUI_Car.Type;
import gui_main.GUI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static gui_fields.GUI_Car.Type.*;
import static gui_fields.GUI_Car.Type.RACECAR;

/**
 * This class implements a view on the Monopoly game based
 * on the original Matador GUI; it serves as a kind of
 * adapter to the Matador GUI. This class realizes the
 * MVC-principle on top of the Matador GUI. In particular,
 * the view implements an observer of the model in the
 * sense of the MVC-principle, which updates the GUI when
 * the state of the game (model) changes.
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 * Sørg for at udvide klassen View, så at dens metode
 */
public class View implements Observer {

    private Game game;
    private GUI gui;

    private Map<Player, GUI_Player> player2GuiPlayer = new HashMap<Player, GUI_Player>();
    private Map<Player, Integer> player2position = new HashMap<Player, Integer>();
    private Map<Space, GUI_Field> space2GuiField = new HashMap<Space, GUI_Field>();
    private Map<Player, PlayerPanel> player2PlayerPanel = new HashMap<Player, PlayerPanel>();
    private boolean disposed = false;

    /**
     * Constructor for the view of a game based on a game and an already
     * running Matador GUI.
     *
     * @param game the game
     * @param gui  the GUI
     */
    public View(Game game, GUI gui) {
        this.game = game;
        this.gui = gui;

        GUI_Field[] guiFields = gui.getFields();

        int i = 0;
        for (Space space : game.getSpaces()) {
            // TODO, here we assume that the games fields fit to the GUI's fields;
            // the GUI fields should actually be created according to the game's
            // fields
            space2GuiField.put(space, guiFields[i]);
            space.attach(this);

            if (space instanceof RealEstate) {
                String line1 = "Leje af grund:";
                String line2 = " m/1 hus:";
                String line3 = " 2 huse:";
                String line4 = " 3 huse:";
                String line5 = " 4 huse:";
                String line6 = " hotel:";
                String line7 = " Pris pr. hus/hotel:";
                String[] lines = {line1, line2, line3, line4, line5, line6, line7};
                String kr = "kr.";

                StringBuffer sb = new StringBuffer();
                for (int j = 0; j < 7; j++) {
                    sb.append(lines[j]);
                    if (j == 6) {
                        String s = ((RealEstate) space).getPriceForHouse() + "";
                        for (int k = 0; k < 21 - s.length() - lines[j].length(); k++) {
                            sb.append("_");
                        }
                        sb.append(s);
                    } else {
                        String s = ((RealEstate) space).getRentLevels()[j] + "";
                        for (int k = 0; k < 21 - s.length() - lines[j].length(); k++) {
                            sb.append("_");
                        }
                        sb.append(s);
                    }
                    sb.append(kr);
                }

                guiFields[i].setDescription(sb + "");
            }
            i++;
        }
    }

    //midlertidigt
    public void addPlayers() {
        for (Player player : game.getPlayers()) {
            PlayerPanel playerPanel = new PlayerPanel(game, player);
            player2PlayerPanel.put(player, playerPanel);
        }
    }

    @Override
    public void update(Subject subject) {
        if (!disposed) {

            if (subject instanceof Player) {
                updatePlayer((Player) subject);
            }
            if (subject instanceof Property) {
                updateProperty((Property) subject);
            }
            if (subject instanceof Game) {
                createPlayers();
            }
        }
    }

    public void updateProperty(Property property) {
        GUI_Field gui_field = this.space2GuiField.get(property);
        if (gui_field instanceof GUI_Ownable) {
            GUI_Ownable gui_ownable = (GUI_Ownable) gui_field;
            Player owner = property.getOwner();
            if (owner != null) {
                gui_ownable.setBorder(owner.getColor());
                gui_ownable.setOwnerName(owner.getName());
            } else {
                gui_ownable.setBorder(null);
                gui_ownable.setOwnerName(null);
            }

            gui_ownable.setRent(property.getRent() + "");
        }
        if (property instanceof RealEstate) {
            GUI_Street street = (GUI_Street) gui_field;
            street.setHotel(false);
            if (((RealEstate) property).getHouseCount() > 0 && ((RealEstate) property).getHouseCount() < 5) {
                street.setHouses(((RealEstate) property).getHouseCount());
            } else if (((RealEstate) property).getHouseCount() == 5) {
                street.setHotel(true);
            }

            /*street.setDescription("Leje af grund: " + property.getRentLevels()[0] + "kr."
                    + "\nm/1 hus: " + property.getRentLevels()[1] + "kr."
                    + "\n2 huse: " + property.getRentLevels()[2] + "kr."
                    + "\n3 huse: " + property.getRentLevels()[3] + "kr."
                    + "\n4 huse: " + property.getRentLevels()[4] + "kr."
                    + "\nhotel: " + property.getRentLevels()[5] + "kr."
                    + "\nHvert hus/hotel koster: " + ((RealEstate) property).getPriceForHouse() + "kr.");*/
        }

        if (property.getOwner() != null) {
            player2PlayerPanel.get(property.getOwner()).update();
        }
    }

    /**
     * This method updates a player's state in the GUI. Right now, this
     * concerns the players position and balance only. But, this should
     * also include other information (being i prison, available cards,
     * ...)
     *
     * @param player the player who's state is to be updated
     */
    private void updatePlayer(Player player) {
        GUI_Player guiPlayer = this.player2GuiPlayer.get(player);
        if (guiPlayer != null) {
            guiPlayer.setBalance(player.getBalance());

            GUI_Field[] guiFields = gui.getFields();
            Integer oldPosition = player2position.get(player);
            if (oldPosition != null && oldPosition < guiFields.length) {
                guiFields[oldPosition].setCar(guiPlayer, false);
            }
            int pos = player.getCurrentPosition().getIndex();
            if (pos < guiFields.length) {
                player2position.put(player, pos);
                guiFields[pos].setCar(guiPlayer, true);
            }

            String name = player.getName();
            if (player.isBroke()) {
            } else if (player.isInPrison()) {
                guiPlayer.setName(player.getName() + " (in prison)");
            }
            if (!name.equals(guiPlayer.getName())) {
                guiPlayer.setName(name);
            }
            player2PlayerPanel.get(player).update();
        }
    }

    public void dispose() {
        if (!disposed) {
            disposed = true;
            for (Player player : game.getPlayers()) {
                // unregister from the player as observer
                player.detach(this);
            }
            for (Space space : game.getSpaces()) {
                space.detach(this);
            }
        }
    }

    /**
     * Nicolai L
     */
    public void createPlayers() {
        TokenColor tokenColor = new TokenColor();
        for (Player player : game.getPlayers()) {
            enterNamePlayer(player);
            Color userColor = chooseCarColor(tokenColor, player);
            player.setColor(userColor);
            GUI_Car car = chosePlayerCar(player);
            GUI_Player guiPlayer = new GUI_Player(player.getName(), player.getBalance(), car);
            player2GuiPlayer.put(player, guiPlayer);
            gui.addPlayer(guiPlayer);
            player2position.put(player, 0);
            player.attach(this);
            updatePlayer(player);
        }
    }

    /**
     * @author Jeppe s170196, Nicolai s185020, Nicolai W s185036
     */
    public void loadPlayers() {
        for (Player player : game.getPlayers()) {
            GUI_Car car = new GUI_Car(player.getColor(), Color.black, Type.valueOf(player.getToken()), Pattern.FILL);
            GUI_Player guiPlayer = new GUI_Player(player.getName(), player.getBalance(), car);
            player2GuiPlayer.put(player, guiPlayer);
            gui.addPlayer(guiPlayer);
            player2position.put(player, player.getCurrentPosition().getIndex());
            player.attach(this);
            updatePlayer(player);
            for (Property p : player.getOwnedProperties()) {
                updateProperty(p);
            }
        }
    }

    /**
     * Nicolai L
     *
     * @param player
     */
    public void enterNamePlayer(Player player) {
        while (true) {
            String input = gui.getUserString("indtast navn");
            if (input.length() > 0) {
                player.setName(input);
                break;
            } else {
/*
                gui.showMessage("prøv igen");
*/
                break;
            }
        }
    }

    /**
     * Nicola L
     *
     * @param player
     * @return
     */
    public GUI_Car chosePlayerCar(Player player) {
        String carChoice = gui.getUserSelection("Choose car", "Car", "Ufo", "Tractor", "Racecar");
        GUI_Car car;
        HashMap<String, GUI_Car.Type> enumMap = new HashMap();
        enumMap.put("Ufo", UFO);
        enumMap.put("Car", CAR);
        enumMap.put("Tractor", TRACTOR);
        enumMap.put("Racecar", RACECAR);
        car = new GUI_Car(player.getColor(), Color.BLUE, enumMap.get(carChoice), GUI_Car.Pattern.FILL);
        player.setToken(enumMap.get(carChoice).toString());
        return car;
    }

    /**
     * Nicolai L
     *
     * @param tokenColorObj
     * @param player
     * @return
     */
    public Color chooseCarColor(TokenColor tokenColorObj, Player player) {
        String[] chooseColorStrings = tokenColorObj.setColorsToChooseFrom().split(" ");
        String carColorS;

        for (int i = 0; i < chooseColorStrings.length; i++) {
        }
        if (chooseColorStrings.length == 6) {
            carColorS = gui.getUserSelection("Choose color", chooseColorStrings[0], chooseColorStrings[1], chooseColorStrings[2], chooseColorStrings[3], chooseColorStrings[4], chooseColorStrings[5]);
        } else if (chooseColorStrings.length == 5) {
            carColorS = gui.getUserSelection("Choose color", chooseColorStrings[0], chooseColorStrings[1], chooseColorStrings[2], chooseColorStrings[3], chooseColorStrings[4]);
        } else if (chooseColorStrings.length == 4) {
            carColorS = gui.getUserSelection("Choose color", chooseColorStrings[0], chooseColorStrings[1], chooseColorStrings[2], chooseColorStrings[3]);
        } else if (chooseColorStrings.length == 3) {
            carColorS = gui.getUserSelection("Choose color", chooseColorStrings[0], chooseColorStrings[1], chooseColorStrings[2]);
        } else if (chooseColorStrings.length == 2) {
            carColorS = gui.getUserSelection("Choose color", chooseColorStrings[0], chooseColorStrings[1]);
        } else {
            gui.showMessage(player.getName() + "'s color is " + chooseColorStrings[0]);
            carColorS = chooseColorStrings[0];
        }
        Color carColorChoice = tokenColorObj.colorChosen(carColorS);

        return carColorChoice;
    }
}
