package game.controller;

import game.database.GameDAO;
import game.model.*;
import game.model.exceptions.GameEndedException;
import game.model.exceptions.PlayerBrokeException;
import game.model.properties.Brewery;
import game.model.properties.RealEstate;
import game.model.properties.Ship;
import game.view.View;
import gui_main.GUI;
import json.JSONUtility;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * The overall controller of a Monopoly game. It provides access
 * to all basic actions and activities for the game. All other
 * activities of the game, should be implemented by referring
 * to the basic actions and activities in this class.
 * <p>
 * Note that this controller is far from being finished and many
 * things could be done in a much nicer and cleaner way! But, it
 * shows the general idea of how the model, view (GUI), and the
 * controller could work with each other, and how different parts
 * of the game's activities can be separated from each other, so
 * that different parts can be added and extended independently
 * from each other.
 * <p>
 * For fully implementing the game, it will probably be necessary
 * to add more of these basic actions in this class.
 * <p>
 * The <code>doAction()</code> methods of the
 * {@link Space} and
 * the {@link Card}
 * can be implemented  on the basic actions and activities
 * of this game controller.
 * based
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 * @author Malte B. Kristensen, s185039@student.dtu.dk
 * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
 * @author Neal P. Norman, 	    s060527@student.dtu.dk
 * @author Jeppe K. Larsen, 	s170196@student.dtu.dk
 * @author Mads M. D. Hemer, 	s170185@student.dtu.dk
 * @author Nicolai J. Larsen, 	s185020@student.dtu.dk
 *
 */
public class GameController {

    private JSONUtility ju = new JSONUtility();
    private Game game;
    private GUI gui;
    private GameDAO gameDb;
    private int sumOfDies;
    private View view;
    private boolean terminated;
    private boolean disposed = false;
    private IDiceCup diceCup;

    /**
     * Constructor for a controller of a game.
     *  TODO: There is a call to super();, but GameController has no superclass, so this call is pointless.
     *  also consider standardising the constructor. Stick to one convention create object in constructor:
     *  private JSONUtility ju = new JSONUtility(); > constructor
     *  @author Neal P. Norman, 	    s060527@student.dtu.dk
     *
     */
    public GameController() {
        super();
        game = ju.createGame();
        gui = new GUI();
        initializeGUI();
        gameDb = new GameDAO();
        diceCup = new DiceCup();
    }

    /**
     * TODO: This method should be moved to the View class, possibly as a part of the existing initializeGUI-method in that class.
     * This method will initialize the GUI by adding descriptions to all properties. As of now, the initialization
     * assumes that the spaces of the game fit to the fields of the GUI;
     * this could eventually be changed, by creating the GUI fields
     * based on the underlying game's spaces (fields).
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void initializeGUI() {
        int i = 0;
        for (Space space : game.getSpaces()) {
            Property property = null;
            if (space instanceof Property) {
                property = (Property) space;
                gui.getFields()[i].setSubText(((Property) space).getCost() + " kr.");
            }
            String description = "";
            if (property instanceof RealEstate) {
                description = "<table width=\"170\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">"
                        + "<tr><td>Leje af grund:</td><td align=\"right\">" + property.getRentLevels()[0] + "kr.</td></tr>"
                        + "<tr><td>m/1 hus:</td><td align=\"right\">" + property.getRentLevels()[1] + "kr.</td></tr>"
                        + "<tr><td>2 huse:</td><td align=\"right\">" + property.getRentLevels()[2] + "kr.</td></tr>"
                        + "<tr><td>3 huse:</td><td align=\"right\">" + property.getRentLevels()[3] + "kr.</td></tr>"
                        + "<tr><td>4 huse:</td><td align=\"right\">" + property.getRentLevels()[4] + "kr.</td></tr>"
                        + "<tr><td>hotel:</td><td align=\"right\">" + property.getRentLevels()[5] + "kr.</td></tr>"
                        + "<tr><td>pr. hus/hotel:</td><td align=\"right\">" + ((RealEstate) space).getPriceForHouse() + "kr.</td></tr></table>";
            } else if (space instanceof Ship) {
                description = "<table width=\"170\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">"
                        + "<tr><td>Leje:</td><td align=\"right\">" + property.getRentLevels()[0] + "kr.</td></tr>"
                        + "<tr><td>2 rederier:</td><td align=\"right\">" + property.getRentLevels()[1] + "kr.</td></tr>"
                        + "<tr><td>3 rederier:</td><td align=\"right\">" + property.getRentLevels()[2] + "kr.</td></tr>"
                        + "<tr><td>4 rederier:</td><td align=\"right\">" + property.getRentLevels()[3] + "kr.</td></tr></table>";
            } else if (space instanceof Brewery) {
                description = ("Hvis 1 virksomhed ejes, betales 100 gange så meget, som øjnene viser. Hvis både Tuborg og Carlsberg ejes, betales 200 gange så meget, som øjnene viser.");
            }
            gui.getFields()[i].setDescription(description);
            gui.getFields()[i].setTitle(space.getName());

            if (space.getIndex() == 10) gui.getFields()[i].setSubText("Fængsel");
            if (space instanceof GoToJail) gui.getFields()[i].setSubText("Gå i fængsel");
            if (space instanceof Chance) gui.getFields()[i].setSubText("Prøv lykken");
            if (space instanceof Tax) gui.getFields()[i].setSubText("Betal 2000");
            i++;
        }
    }

    /**
     * This method calls the GUI to show the first dialogue with the user.
     * Shows a menu, where the user can choose to either start a new game, load a game, or exit the application.
     * The method recurses infinitly, so that the menu is automatically shown when a game is ended og exited.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     * @author Nicolai J. Larsen, 	s185020@student.dtu.dk
     */
    public void playOrLoadGame() {
        String userSelection = gui.getUserButtonPressed("", "Start nyt spil", "Hent spil", "Afslut");
        if (userSelection.substring(0, 5).equalsIgnoreCase("start")) {
            createPlayers();
            play();
        } else if (userSelection.equals("Afslut")) {
            System.exit(0);
        } else {
            String gameSelection = chooseFromOptions(gameDb.getGamesList(), "Vælg spil:", "Annuller", null, null, null);
            if (gameSelection != null) {
                game = ju.createGame();
                game = gameDb.loadGame(game, gameSelection);
                play();
            }
        }
        playOrLoadGame();
    }

    /**
     * Asks the user how many players, they are.
     * Then asks each player for their name, their chosen color and chosen type of vehicle.
     * Sets the data for each player object according to their choices.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     * @author Neal P. Norman, 	    s060527@student.dtu.dk
     */
    private void createPlayers() {
        //Create a new game.
        game = ju.createGame();
        //Ask for number of players with -chooseFromOptions- 3 to 6 players
        ArrayList<Integer> options = new ArrayList<>(Arrays.asList(3, 4, 5, 6));
        Integer numOfPlayers = chooseFromOptions(options, "Hvor mange spillere?", "Annuller", null, null, null);
        if (numOfPlayers != null) {
            game.createPlayers(numOfPlayers);

            ArrayList<Player.PlayerColor> colorsChosen = new ArrayList<>();
            ArrayList<String> namesChosen = new ArrayList<>();
            for (int i = 0; i < numOfPlayers; i++) {
                Player player = game.getPlayers().get(i);
                boolean validInput = false;
                while(!validInput) {
                    //Enter names of current of player in for loop TODO input validation
                    String name = gui.getUserString("Indtast navn på spiller " + (i + 1) + ":");
                    if (name.length() > 0 && !namesChosen.contains(name)) {
                        player.setName(name);
                        namesChosen.add(name);
                        validInput = true;
                    } else if (namesChosen.contains(name)) {
                        gui.showMessage("Dette navn er allerede taget. Vælg et andet.");
                    }
                }
                //Choose colour of player in for loop
                //Creates new array of colour options, without the chosen colours
                //Then allows player to choose from new array.
                ArrayList<String> colorOptions = new ArrayList<>();
                for (Player.PlayerColor color : Player.PlayerColor.values()) {
                    if (!colorsChosen.contains(color)) {
                        colorOptions.add(color.toString());
                    }
                }
                String playerColor = gui.getUserSelection("Vælg farve:", colorOptions.toArray(new String[colorOptions.size()]));
                Player.PlayerColor chosenColor = Player.PlayerColor.getColorFromString(playerColor);
                colorsChosen.add(chosenColor);
                player.setColorEnumType(chosenColor);
                //Here the player choose car type.
                String[] carTypes = new String[Player.CarType.values().length];
                for (int j = 0; j < carTypes.length; j++) {
                    carTypes[j] = Player.CarType.values()[j].toString();
                }
                String playerCar = gui.getUserSelection("Vælg køretøj:", carTypes);
                player.setCarType(Player.CarType.getCarTypeFromString(playerCar));
            }
        }
    }

    /**
     * The main method to start the game. The game is started with the
     * current player of the game; this makes it possible to resume a
     * game at any point.
     */
    public void play() {

        //TODO: Maybe the cards should not be shuffled when loading a game – but loaded from the database?
        game.shuffleCardDeck();
        view = new View(game, gui);
        view.initializeGUI();

        List<Player> players = game.getPlayers();
        Player c = game.getCurrentPlayer();

        int current = 0;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (c.equals(p)) {
                current = i;
            }
        }

        terminated = false;
        while (!terminated) {
            Player player = players.get(current);
            if (!player.isBroke()) {
                try {
                    showTurnMenu(player);
                    if (!terminated) this.makeMove(player);
                } catch (PlayerBrokeException e) {
                } catch (GameEndedException w) {
                    //gui.showMessage(w.getMessage());
                    if (winner())
                        terminated = true;
                } finally {

                }
            }
            current = (current + 1) % players.size();
            game.setCurrentPlayer(players.get(current));
        }

        //When the game is terminated (either by ending og exiting), the view is disposed,
        //and then a new game is instantiated, ready to be started either as a new game or a loaded game.
        resetViewAndGUI();
        game = null;
    }

    /**
     * Show the basic turn menu to player.
     *
     * Menu consists of: bygge, sælge, handle and pantsætte, save game and close game.
     * Shows a menu with different buttons for each function of the game, which is:
     * Build houses/hotels, trade, sell houses, mortgage/unmortage, save game,
     * close game, and "drive" (as in throw the dice and move the token).
     * Each player (in real life) may use these options whenever they wish.
     *
     * @param player the player passed in to take turn.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void showTurnMenu(Player player) {
        boolean continueChoosing = true;
        while (continueChoosing) {
            String choice = gui.getUserButtonPressed("Det er " + player.getName() + "'s tur. Alle spillere må bygge, sælge, handle og pantsætte. Hvad skal der ske?", "Byg huse", "Sælg huse", "Handel", "Pantsættelser", "Gem spil", "Luk spil", "Kør");
            switch (choice) {
                case "Byg huse":
                    buyHouseAction();
                    break;
                case "Handel":
                    trade(null);
                    break;
                case "Sælg huse":
                    sellHouseAction(null);
                    break;
                case "Pantsættelser":
                    String input = gui.getUserButtonPressed("Vælg:", "Pantsætte", "Indfri gæld", "Tilbage til menu");
                    if (input.equals("Pantsætte")) {
                        mortgageAction(null);
                    } else if (input.equals("Indfri gæld")) {
                        unmortgageAction();
                    }
                    break;
                case "Gem spil":
                    saveGame();
                    break;
                case "Luk spil":
                    terminateGame();
                    continueChoosing = false;
                    break;
                default:
                    continueChoosing = false;
                    break;
            }
        }
    }

    /**
     * Saves the current game to the database
     *
     */
    public void saveGame() {
        if (game.getGameId() < 0) {
            gameDb.saveGame(game);
        } else {
            gameDb.updateGame(game);
        }
        playSound("saved.wav");
        gui.showMessage("Spillet blev gemt!");
    }

    /**
     * Checks if one or less players are broke
     * @return boolean This returns true when the game is over.
     */
    public boolean gameEnds() {
        boolean returnBool = false;

        int countBroke = 0;
        for (Player p : game.getPlayers()) {
            if (p.isBroke()) {
                countBroke++;
            }
        }
        if (countBroke >= game.getPlayers().size() - 1) {
            returnBool = true;

        }
        return returnBool;
    }

    /**
     * Checks whether there is a player that isn't broke.
     * displays message with specific message when activated.
     * @return boolean, This returns true when there is a winner
     */
    public boolean winner() {
        // Check whether we have a winner
        boolean returnBool = false;
        Player winner = null;
        int countActive = 0;
        for (Player p : game.getPlayers()) {
            if (!p.isBroke()) {
                countActive++;
                winner = p;
            }
        }
        if (countActive == 1) {
            playSound("applause.wav");
            gui.showMessage("Tillykke, " + winner.getName() + "! Du har vundet spillet med en balance på " + winner.getBalance() + "kr!");
            returnBool = true;
        } else if (countActive < 1) {
            // This can actually happen in very rare conditions and only
            // if the last player makes a stupid mistake (like buying something
            // in an auction in the same round when the last but one player went
            // bankrupt)
            gui.showMessage(
                    "Spillet er slut uden en vinder, da alle spillere er gået konkurs!");
            returnBool = false;
        }
        return returnBool;
    }

    /**
     * Terminates the game, so that the user will return to the "main menu".
     */

    private void terminateGame() {
        terminated = true;
    }

    /**
     * Disposes the view and closes all open JFrames (the GUI and player panels).
     * Lastly instantiates a new gui and initializes it.
     */
    private void resetViewAndGUI() {
        view.dispose();
        view = null;
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            frame.dispose();
        }
        gui = new GUI();
        initializeGUI();
    }

    /**
     * This method implements a activity of a single move of the given player.
     * It throws a {@link PlayerBrokeException}
     * if the player goes broke in this move.
     * Takes into account actions required if player is in prison.
     * Rolls the dice
     *
     *
     * @param player the player making the move
     * @throws PlayerBrokeException if the player goes broke during the move
     * @author Neal P. Norman, 	s060527@student.dtu.dk
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void makeMove(Player player) throws PlayerBrokeException, GameEndedException {
        boolean castDouble;
        int doublesCount = 0;

        if (player.isInPrison() && player.getOwnedCards().size() > 0) {
            String choice = gui.getUserButtonPressed(player + ", ønsker du at bruge dit benådelseskort til at blive løsladt?", "Ja", "Nej");
            if (choice.equals("Ja")) {
                useCardToGetOut(player);
            }
        } else if (player.isInPrison() && player.getDoubleAttempts() < 3) {
            String choice = gui.getUserButtonPressed(player + ", ønsker du at betale bøden på 1000 kr for at blive løsladt fra fængsel?", "Ja", "Nej");
            if (choice.equals("Ja")) {
                payToGetOut(player);
            }
        } else if (player.isInPrison() && player.getDoubleAttempts() == 3) {
            gui.showMessage(player + ", hvis du ikke slår to ens denne gang, skal du betale bøden på 1000 kr og derefter rykke frem.");
        }
        do {
            diceCup.rollDice();
            int die1 = diceCup.getDice()[0];
            int die2 = diceCup.getDice()[1];
            sumOfDies = die1 + die2;
            castDouble = (die1 == die2);
            gui.setDice(die1, die2);

            if (player.isInPrison() && castDouble) {
                player.setInPrison(false);
                gui.showMessage(player + " har kastet to ens, og bliver derfor løsladt fra fængslet!");
            } else if (player.isInPrison() && player.getDoubleAttempts() < 3) {
                player.setDoubleAttempts(player.getDoubleAttempts() + 1);
                gui.showMessage(player + " forbliver i fængsel, da han/hun ikke slog to ens.");
            } else if (player.isInPrison() && player.getDoubleAttempts() == 3) {
                gui.showMessage(player + ", du slog desværre ikke to ens, og du skal derfor betale bøden på 1000 kr for at blive løsladt.");
                payToGetOut(player);
            }
            if (castDouble) {
                doublesCount++;
                if (doublesCount > 2) {
                    gui.showMessage(player + " har kastet to ens tre gange i træk og ryger derfor i fængsel!");
                    gotoJail(player);
                    return;
                }
            }
            if (!player.isInPrison()) {
                // make the actual move by computing the new position and then
                // executing the action moving the player to that space
                player.setDoubleAttempts(0);
                int pos = player.getCurrentPosition().getIndex();
                List<Space> spaces = game.getSpaces();
                int newPos = (pos + die1 + die2) % spaces.size();
                Space space = spaces.get(newPos);
                playSound("engine.wav");
                moveToSpace(player, space);
                if (castDouble) {
                    gui.showMessage(player + " har kastet to ens og får derfor en ekstra tur.");
                    showTurnMenu(player);
                    if (terminated) return;
                }
            }
        } while (castDouble);
    }

    /**
     * This method is called by the makeamove when a player wants to pay to get out of jail.
     * @param player that has chosen to pay to get out of jail.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     * @author Neal P. Norman, 	s060527@student.dtu.dk
     */
    private void payToGetOut(Player player) {
        try {
            //TODO: The 1000 should probably not be hardcoded.
            paymentToBank(player, 1000);
        } catch (PlayerBrokeException e) {
            return;
        } catch (GameEndedException e) {
            e.printStackTrace();
        }
        player.setInPrison(false);
        gui.showMessage(player + "blev løsladt fra fængsel og må nu rykke frem!");
    }

    /**
     * Used when a player uses an owned getOutOfJail-card to get out of jail.
     * @param player
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void useCardToGetOut(Player player) {
        try {
            ArrayList<Card> ownedCards = new ArrayList<>(player.getOwnedCards());
            returnChanceCardToDeck(ownedCards.get(0));
            ownedCards.remove(0);
            player.setOwnedCards(ownedCards);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method implements the activity of moving the player to the new position,
     * including all actions associated with moving the player to the new position.
     *
     * @param player the moved player
     * @param space  the space to which the player moves
     * @throws PlayerBrokeException when the player goes broke doing the action on that space
     * @throws GameEndedException if the game ends
     */
    public void moveToSpace(Player player, Space space) throws PlayerBrokeException, GameEndedException {
        int posOld = player.getCurrentPosition().getIndex();
        player.setCurrentPosition(space);

        if (posOld > player.getCurrentPosition().getIndex()) {
            // Note that this assumes that the game has more than 12 spaces here!
            // TODO: the amount of 2000$ should not be a fixed constant here (could also
            //       be configured in the Game class.
            playSound("yougotmoney.wav");
            gui.showMessage(player + " modtager 2000kr for at passere start!");
            this.paymentFromBank(player, 2000);
        }
        gui.showMessage(player + " ankommer til " + space + ".");

        // Execute the action associated with the respective space. Note
        // that this is delegated to the field, which implements this action
        space.doAction(this, player);
    }

    /**
     * The method implements the action of a player going directly to jail.
     *
     * @param player the player going to jail
     */
    public void gotoJail(Player player) {
        // Field #10 is in the default game board of Monopoly the field
        // representing the prison.
        // TODO the 10 should not be hard coded
        playSound("imprisoned.wav");
        player.setCurrentPosition(game.getSpaces().get(10));
        player.setInPrison(true);
    }

    /**
     * The method implementing the activity of taking a chance card.
     *
     * @param player the player taking a chance card
     * @throws PlayerBrokeException if the player goes broke by this activity
     */
    public void takeChanceCard(Player player) throws PlayerBrokeException, GameEndedException {

        Card card = game.drawCardFromDeck();

        gui.displayChanceCard(card.getText());
        gui.showMessage(player + " trækker et chancekort...");

        try {
            card.doAction(this, player);
        } finally {
            //gui.displayChanceCard("done");
        }
        gui.displayChanceCard("");
    }

    /**
     * This method implements the action returning a drawn card or a card keep with
     * the player for some time back to the bottom of the card deck.
     *
     * @param card returned card
     */
    public void returnChanceCardToDeck(Card card) {
        game.returnCardToDeck(card);
    }

    /**
     * This method implements the activity where a player can obtain
     * cash by selling houses back to the bank, by mortgaging own properties,
     * or by selling properties to other players. This method is called, whenever
     * the player does not have enough cash available for an action. If
     * the player does not manage to free at least the given amount of money,
     * the player will be broke; this is to help the player make the right
     * choices for freeing enough money.
     *
     * @param player the player
     * @param amount the amount the player should have available after the act
     * @param beforePurchase when set to true, this method handles the situation, where a player
     *                       wants to try to obtain cash before purchasing a property.
     *                       When false, handles the situation where a player is about to
     *                       go broke.
     * @author Nicolai Wulff, s185036@student.dtu.dk
     *
     */
    public boolean obtainCash(Player player, int amount, boolean beforePurchase) {
        boolean tryToObtain = true;
        while (tryToObtain) {

            //Check if the player has anything left to sell. If not, and the player still can't pay,
            //declare the player bankrupt.
            if (player.getOwnedProperties().size() == 0 && player.getBalance() < amount && !beforePurchase) {
                gui.showMessage(player + ", du har desværre intet tilbage, du kan sælge, og må derfor erklæres konkurs.");
                return false;
            }

            //Shows a menu and message that correspond to the situation of the player.
            String choice;
            if (!beforePurchase) {
                if (player.getBalance() < amount) {
                    choice = gui.getUserButtonPressed(player + ", du har i øjeblikket ikke nok penge til at betale " + amount + "kr. Hvad vil du gøre?", "Sælg huse", "Pantsæt grunde", "Handle", "Erklær dig konkurs");
                } else {
                    choice = gui.getUserButtonPressed(player + ", du har nu råd til at betale " + amount + "kr. Du kan nu fortsætte med at sælge/handle eller gå til betaling.", "Sælg huse", "Pantsæt grunde", "Handle", "Betal");
                }
            } else {
                if (player.getBalance() < amount) {
                    choice = gui.getUserButtonPressed(player + ", du har i øjeblikket ikke nok penge til at købe grunden for " + amount + "kr. Hvad vil du gøre?", "Sælg huse", "Pantsæt grunde", "Handle", "Annuller købet");
                } else {
                    choice = gui.getUserButtonPressed(player + ", du har nu råd til at købe grunden for " + amount + "kr. Du kan nu fortsætte med at sælge/handle eller gå til betaling.", "Sælg huse", "Pantsæt grunde", "Handle", "Køb", "Annuller købet");
                }
            }

            switch (choice) {
                case "Sælg huse":
                    sellHouseAction(player);
                    break;
                case "Pantsæt grunde":
                    mortgageAction(player);
                    break;
                case "Handle":
                    trade(player);
                    break;
                case "Erklær dig konkurs":
                    if (player.getOwnedPropertiesNotMortgaged().size() > 0) {
                        boolean ownsHouses = false;
                        for (Property property : player.getOwnedPropertiesNotMortgaged()) {
                            if (property instanceof RealEstate) {
                                if (((RealEstate) property).getHouseCount() > 0) ownsHouses = true;
                            }
                        }
                        gui.showMessage(ownsHouses ? "Du har stadig huse/hoteller, du kan sælge!" : "Du har stadig grunde, du kan pantsætte eller handle med!");
                    } else if (player.getOwnedProperties().size() > 0 && player.getOwnedPropertiesNotMortgaged().size() == 0) {
                        String choice2 = gui.getUserButtonPressed("Er du sikker på, du ikke vil forsøge at sælge dine pantsatte grunde til andre spillere?", "Ja", "Nej");
                        if (choice2.equals("Ja")) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    break;
                case "Annuller købet":
                    return false;
                case "Betal":
                    return true;
                case "Køb":
                    return true;
            }
        }
        return true;
    }

    /**
     * This method implements the activity of offering a player to buy
     * a property. This is typically triggered by a player arriving on
     * an property that is not sold yet. If the player chooses not to
     * buy, the property will be set for auction.
     *
     * @param property the property to be sold
     * @param player   the player the property is offered to
     * @throws PlayerBrokeException when the player chooses to buy but could not afford it
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void offerToBuy(Property property, Player player) throws PlayerBrokeException, GameEndedException {

        String choice;
        boolean proceedPurchase = true;
        if (player.getBalance() < property.getCost()) {
            choice = gui.getUserButtonPressed(player + ", du har i øjeblikket ikke penge nok til at købe " + property + ". Vil du forsøge at finde pengene ved at sælge, handle eller pantsætte noget?", "Ja", "Nej");
            if (choice.equals("Ja")) {
                proceedPurchase = obtainCash(player, property.getCost(), true);
            } else {
                proceedPurchase = false;
            }
        }

        choice = "";
        if (proceedPurchase)
            choice = gui.getUserSelection(player + ", ønsker du at købe " + property + " for " + property.getCost() + "kr?", "Ja", "Nej");

        if (choice.equals("Ja")) {
            try {
                paymentToBank(player, property.getCost());
            } catch (PlayerBrokeException e) {
                // if the payment fails due to the player being broke,
                // an auction (among the other players is started
                player.setBroke(true);
                auction(property);
                // then the current move is aborted by casting the
                // PlayerBrokeException again
                throw e;
            }
            property.setOwner(player);
            player.addOwnedProperty(property);
            playSound("purchase.wav");
            return;
        }

        // In case the player does not buy the property,
        // an auction is started
        auction(property);
    }

    /**
     * This method implements a payment activity to another player,
     * which involves the player to obtain some cash on the way, in case he does
     * not have enough cash available to pay right away. If he cannot free
     * enough money in the process, the player will go bankrupt.
     *
     * @param payer    the player making the payment
     * @param amount   the payed amount
     * @param receiver the beneficiary of the payment
     * @throws PlayerBrokeException when the payer goes broke by this payment
     */
    public void payment(Player payer, int amount, Player receiver) throws PlayerBrokeException, GameEndedException {
        if (payer.getBalance() < amount) {
            obtainCash(payer, amount, false);
            if (payer.getBalance() < amount) {
                playerBrokeTo(payer, receiver);
                if (gameEnds())
                    throw new GameEndedException();
                throw new PlayerBrokeException(payer);
            }
        }
        gui.showMessage(payer + " betaler " + amount + "kr til " + receiver + ".");
        payer.payMoney(amount);
        receiver.receiveMoney(amount);
    }

    /**
     * This method implements the action of a player receiving money from
     * the bank.
     *
     * @param player the player receiving the money
     * @param amount the amount recieved from the bank
     */
    public void paymentFromBank(Player player, int amount) {
        player.receiveMoney(amount);
    }

    /**
     * This method implements the activity of a player making a payment to
     * the bank. Note that this might involve the player to obtain some
     * cash; in case he cannot free enough cash, he will go bankrupt
     * to the bank.
     *
     * @param player the player making the payment
     * @param amount the amount
     * @throws PlayerBrokeException when the player goes broke by the payment
     */
    public void paymentToBank(Player player, int amount) throws PlayerBrokeException, GameEndedException {
        if (amount > player.getBalance()) {
            obtainCash(player, amount, false);
            if (amount > player.getBalance()) {
                playerBrokeToBank(player);
                if (gameEnds())
                    throw new GameEndedException();
                throw new PlayerBrokeException(player);
            }
        }
        gui.showMessage(player + " betaler " + amount + "kr til banken.");
        player.payMoney(amount);
    }

    /**
     * This method implements the activity of auctioning a property.
     *
     * @param property the property which is for auction
     *
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     * @author Jeppe K. Larsen, 	s170196@student.dtu.dk
     * @author Mads M. D. Hemer, 	s170185@student.dtu.dk
     *
     */
    public void auction(Property property) throws GameEndedException {
        List<Player> bidders = new ArrayList<>();
        Collections.shuffle(bidders);
        for (Player p : game.getActivePlayers(false)) {
            if (p.getBalance() > 0) {
                bidders.add(p);
            }
        }

        if (bidders.size() == 0) {
            gui.showMessage("Der kan ikke afholdes en auktion for salget af " + property + ", da ingen spillere kan deltage. ");
            return;
        }

        gui.showMessage("Der vil nu blive afholdt en auktion for at købe " + property + ". Grunden er vurderet til " + property.getCost() + "kr. Man udgår af auktionen ved at byde 0kr eller ikke at byde over højeste bud.");

        if (bidders.size() == 1) {
            gui.showMessage(bidders.get(0) + ", du er en eneste, der kan deltage i auktionen. Du får derfor lov til selv at bestemme, hvad du vil betale for " + property + ".");
        } else {
            for (Player p : game.getActivePlayers(true)) {
                if (p.isInPrison()) {
                    gui.showMessage(p + " kan ikke deltage i auktionen, da han/hun er i fængsel.");
                } else if (p.getBalance() == 0) {
                    gui.showMessage(p + " kan ikke deltage i auktionen, da han/hun ikke har nogen penge.");
                }
            }
        }

        Clip clip = null;
        try {
            File f = new File("src/resources/sounds/auction.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(f.toURI().toURL());
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int highBid = 0;
        Player highBidder = null;

        do {
            ArrayList<Player> toBeRemoved = new ArrayList<>();
            for (Player bidder : bidders) {
                if (highBidder == bidder) continue;
                int bid = -1;
                while (!(bid >= 0 && bid <= bidder.getBalance())) {
                    if (highBid == 0) {
                        bid = gui.getUserInteger(bidder + ", hvad vil du byde?", 0, bidder.getBalance());
                    } else {
                        bid = gui.getUserInteger(bidder + ", hvad vil du byde?. Højeste bud er: " + highBid + "kr af " + highBidder, 0, bidder.getBalance());
                    }

                    if (bid < 0 || bid > bidder.getBalance()) {
                        gui.showMessage("Ugyldigt bud! Prøv igen.");
                    } else if (bid > highBid) {
                        highBid = bid;
                        highBidder = bidder;
                    } else if (bid == 0) {
                        gui.showMessage(bidder + " har budt 0kr og udgår derfor af auktionen.");
                        toBeRemoved.add(bidder);
                    } else {
                        gui.showMessage(bidder + " har ikke budt over og udgår derfor af auktionen.");
                        toBeRemoved.add(bidder);
                    }
                }
            }
            bidders.removeAll(toBeRemoved);
        } while (bidders.size() > 1);

        if (bidders.size() == 0) {
            clip.stop();
            gui.showMessage("Ingen spillere har budt på " + property + ", og den er derfor stadig til salg.");
            return;
        }

        clip.stop();
        playSound("auctionsold.wav");
        gui.showMessage("Første.. Anden.. Tredje.. " + highBidder + " vinder auktionen og køber " + property + " for " + highBid + "kr.");
        try {
            paymentToBank(highBidder, highBid);
            property.setOwner(highBidder);
            highBidder.addOwnedProperty(property);
        } catch (PlayerBrokeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Action handling the situation when one player is broke to another
     * player. All money and properties are transferred to the other player.
     *
     * @param brokePlayer the broke player
     * @param benificiary the player who receives the money and assets
     *
     */
    public void playerBrokeTo(Player brokePlayer, Player benificiary) {
        int amount = brokePlayer.getBalance();
        benificiary.receiveMoney(amount);
        brokePlayer.setBalance(0);
        brokePlayer.setBroke(true);

        // TODO We assume here, that the broke player has already sold all his houses! But, if
        // not, we could make sure at this point that all houses are removed from
        // properties (properties with houses on are not supposed to be transferred, neither
        // in a trade between players, nor when  player goes broke to another player)
        for (Property property : brokePlayer.getOwnedProperties()) {
            property.setOwner(benificiary);
            benificiary.addOwnedProperty(property);
        }
        brokePlayer.removeAllProperties();

        while (!brokePlayer.getOwnedCards().isEmpty()) {
            game.returnCardToDeck(brokePlayer.getOwnedCards().get(0));
        }

        playSound("trombones.wav");
        gui.showMessage(brokePlayer + " er gået konkurs og overfører hele konkursboet til " + benificiary);
    }

    /**
     * Action handling the situation when a player is broke to the bank.
     *
     * @param player the broke player
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void playerBrokeToBank(Player player) throws GameEndedException {

        player.setBalance(0);
        player.setBroke(true);

        player.removeAllProperties();

        playSound("trombones.wav");
        gui.showMessage(player + " er gået konkurs.");

        for (Property property : player.getOwnedProperties()) {
            property.setOwner(null);
            if (property.getMortgaged()) property.setMortgaged(false);
            auction(property);
        }

        while (!player.getOwnedCards().isEmpty()) {
            game.returnCardToDeck(player.getOwnedCards().get(0));
        }
    }

    /**
     * Method for disposing of this controller and cleaning up its resources.
     */
    public void dispose() {
        if (!disposed && view != null) {
            disposed = true;
            if (view != null) {
            }
        }
    }

    /**
     * Buys a house, if the player can afford it.
     *
     * @param player player that is buying the property
     * @param re Realestate object of property that is being bought
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void buyHouse(Player player, RealEstate re) {
        int lowestHouseCount = 5;
        for (Property property : player.getOwnedProperties()) {
            if (property.getColorGroup() == re.getColorGroup() && ((RealEstate) property).getHouseCount() < lowestHouseCount) {
                lowestHouseCount = ((RealEstate) property).getHouseCount();
            }
        }
        if (re.getHouseCount() == 5) {
            gui.showMessage("Du kan ikke bygge mere på denne grund!");
        } else if (re.getHouseCount() > lowestHouseCount) {
            gui.showMessage("Du skal bygge jævnt!");
        } else if (player.getBalance() >= re.getPriceForHouse()) {
            try {
                paymentToBank(player, re.getPriceForHouse());
                re.setHouseCount(re.getHouseCount() + 1);
                playSound("purchase.wav");
            } catch (PlayerBrokeException e) {
                e.printStackTrace();
            } catch (GameEndedException e) {
                e.printStackTrace();
            }
        } else {
            gui.showMessage(player.getName() + ", du har ikke råd til at bygge et hus/hotel på denne grund.");
        }
    }

    /**
     * @param player player that is selling the property
     * @param re Realestate object of property that is being sold
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void sellHouse(Player player, RealEstate re) {
        int highestHouseCount = 0;
        for (Property property : player.getOwnedProperties()) {
            if (property.getColorGroup() == re.getColorGroup() && ((RealEstate) property).getHouseCount() > highestHouseCount) {
                highestHouseCount = ((RealEstate) property).getHouseCount();
            }
        }
        if (re.getHouseCount() == 0) {
            gui.showMessage("Du har ingen huse på denne grund!");
        } else if (re.getHouseCount() < highestHouseCount) {
            gui.showMessage("Du skal sælge dine huse jævnt!");
        } else {
            paymentFromBank(player, re.getPriceForHouse() / 2);
            re.setHouseCount(re.getHouseCount() - 1);
        }
    }

    /**
     * Asks the player, if he/she wants to build houses, if the player owns any real estate.
     *
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void buyHouseAction() {
        Player player = choosePlayer("Hvilken spiller ønsker at købe huse?", null, false);
        if (player == null) return;
        boolean continueBuying = true;
        while (continueBuying) {
            ArrayList<RealEstate> potentialProperties = new ArrayList<>();
            ArrayList<Integer> housePrices = new ArrayList<>();
            ArrayList<String> houseOrHotel = new ArrayList<>();
            for (Property property : player.getOwnedProperties()) {
                if (property instanceof RealEstate && property.getSuperOwned()) {
                    if (((RealEstate) property).getHouseCount() < 5) {
                        potentialProperties.add((RealEstate) property);
                        housePrices.add(((RealEstate) property).getPriceForHouse());
                        if (((RealEstate) property).getHouseCount() < 4) {
                            houseOrHotel.add("kr/hus");
                        } else if (((RealEstate) property).getHouseCount() == 4) {
                            houseOrHotel.add("kr/hotel");
                        }
                    }
                }
            }

            RealEstate re = chooseFromOptions(
                    potentialProperties,
                    "På hvilken grund vil du købe et hus/hotel?",
                    "Stop med at købe",
                    ", ",
                    housePrices,
                    houseOrHotel);
            if (re == null) {
                continueBuying = false;
            } else {
                buyHouse(player, re);
            }
        }
    }

    /**
     * Asks the user which player wants to sell houses and which property he/she wants to sell houses from.
     * @param player if not null, the player is then already chosen, and the user will not be prompted to choose player.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void sellHouseAction(Player player) {
        if (player == null) player = choosePlayer("Hvilken spiller ønsker at sælge huse?", null, false);
        if (player == null) return;
        boolean continueSelling = true;
        while (continueSelling) {
            //Makes a list of all real estate, that has at least one house built on it.
            ArrayList<RealEstate> propertyOptions = new ArrayList<>();
            ArrayList<Integer> housePrices = new ArrayList<>();
            ArrayList<String> houseOrHotel = new ArrayList<>();
            for (Property property : player.getOwnedProperties()) {
                if (property instanceof RealEstate) {
                    if (((RealEstate) property).getHouseCount() > 0) {
                        propertyOptions.add((RealEstate) property);
                        housePrices.add(((RealEstate) property).getPriceForHouse() / 2);
                        if (((RealEstate) property).getHouseCount() < 5) {
                            houseOrHotel.add("kr/hus");
                        } else if (((RealEstate) property).getHouseCount() == 5) {
                            houseOrHotel.add("kr/hotel");
                        }
                    }
                }
            }

            RealEstate re = chooseFromOptions(
                    propertyOptions,
                    "Fra hvilken grund ønsker du at sælge huse?",
                    "Stop med at sælge",
                    ", ",
                    housePrices,
                    houseOrHotel);
            if (re == null) {
                continueSelling = false;
            } else {
                sellHouse(player, re);
            }
        }
    }

    /**
     * This method implements the activity of trading between two players.
     * This method assumes that the players negotiate the deal in real life. Thereafter, they
     * may enter what the deal is (how many money, properties and cards will be traded).
     * @param firstParty if not null, the first party of the trade will already be chosen, when the method is called.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void trade(Player firstParty) {
        Player[] tradingPlayers = new Player[2];
        tradingPlayers[0] = firstParty;
        int[] moneyInOffers = new int[2];
        ArrayList<Property>[] propertiesInOffers = new ArrayList[2];
        propertiesInOffers[0] = new ArrayList<>();
        propertiesInOffers[1] = new ArrayList<>();
        int[] cardsInOffers = new int[2];

        //Keep asking which players wants to trade. Check if each player is in jail).
        boolean choosing = true;
        int i = 0;
        while (choosing) {
            Player player = null;
            if (firstParty != null) i++;
            if (i == 0 && firstParty == null) player = choosePlayer("Hvem er den ene part i handlen?", null, true);
            if (i == 1) player = choosePlayer("Hvem er den anden part i handlen?", tradingPlayers[0], true);
            if (player != null && player.isInPrison()) {
                gui.showMessage(player.getName() + " er i fængsel, og må derfor ikke handle!");
            } else if (player != null) {
                tradingPlayers[i] = player;
                if (i == 1) choosing = false;
                i++;
            } else {
                return;
            }
        }

        //Loop through the two trading players.
        for (int j = 0; j < 2; j++) {
            //Ask what amount of money, the player wants to trade.
            moneyInOffers[j] = gui.getUserInteger(tradingPlayers[j] + ", hvilket beløb vil du tilføje i handlen?", 0, tradingPlayers[j].getBalance());
            if (moneyInOffers[j] < 0) moneyInOffers[j] = 0;

            //Keep adding properties to the trade, until the player chooses to stop.
            boolean continueAdding = true;
            while (continueAdding) {

                ArrayList<Property> propertyOptions = new ArrayList<>();
                for (Property p : tradingPlayers[j].getOwnedProperties()) {
                    if (!propertiesInOffers[j].contains(p)) propertyOptions.add(p);
                }

                Property property = chooseFromOptions(propertyOptions, tradingPlayers[j] + ", hvilke grunde vil du tilføje til handlen?", "Stop med at tilføje grunde", null, null, null);
                if (property == null) {
                    continueAdding = false;
                } else {
                    //Check if the chosen property or any property with the same color group has any houses.
                    //If they do, refuse to trade it, and tell the player to sell their houses first.
                    boolean tradeAble = true;
                    for (Property p : tradingPlayers[j].getOwnedProperties()) {
                        if (p instanceof RealEstate && p.getColorGroup() == property.getColorGroup()) {
                            if (((RealEstate) p).getHouseCount() > 0) {
                                gui.showMessage("Du kan ikke handle med grunde, hvor du har bygget huse i dens farvegruppe.\nSælg din huse i gruppen, før du handler.");
                                tradeAble = false;
                            }
                        }
                    }

                    if (tradeAble) {
                        propertiesInOffers[j].add(property);
                    }
                }
            }

            if (tradingPlayers[j].getOwnedCards().size() > 0) {
                String[] cardOptions = new String[tradingPlayers[j].getOwnedCards().size() + 1];
                for (i = 0; i <= tradingPlayers[j].getOwnedCards().size(); i++) {
                    cardOptions[i] = i + "";
                }
                cardsInOffers[j] = Integer.parseInt(gui.getUserSelection(tradingPlayers[j] + ", hvor mange benådelseskort vil du tilføje til handlen?", cardOptions));
            }
        }

        //Construct a string for each player, containing all the properties,
        // that each player added to the trade, seperated by commas.
        String[] playerProperties = {"", ""};
        for (i = 0; i < 2; i++) {
            for (int j = 0; j < propertiesInOffers[i].size(); j++) {
                playerProperties[i] += propertiesInOffers[i].get(j);
                if (j != 0) {
                    playerProperties[i] += ", ";
                }
            }
        }


        //Construct a string showing a complete overview of the trade.
        String tradeOverview = tradingPlayers[0].getName() + ": " + moneyInOffers[0] + "kr.\n"
                + tradingPlayers[1].getName() + ": " + moneyInOffers[1] + "kr.\n"
                + tradingPlayers[0].getName() + ": " + playerProperties[0] + "\n"
                + tradingPlayers[1].getName() + ": " + playerProperties[1] + "\n"
                + tradingPlayers[0].getName() + ": " + cardsInOffers[0] + " benådelseskort.\n"
                + tradingPlayers[1].getName() + ": " + cardsInOffers[1] + " benådelseskort.\n";

        //Ask if the players want to accept and complete the trade.
        String accept = gui.getUserButtonPressed("Jeres byttehandel ser således ud:\n" + tradeOverview + "\nVil I acceptere og lukke handlen?", "Ja", "Nej");

        //If yes, make the actual trade (exchange money and properties).
        if (accept.equals("Ja")) {
            try {
                payment(tradingPlayers[0], moneyInOffers[0], tradingPlayers[1]);
                payment(tradingPlayers[1], moneyInOffers[1], tradingPlayers[0]);

                for (Property property : propertiesInOffers[0]) {
                    transferProperty(tradingPlayers[0], property, tradingPlayers[1]);
                }
                for (Property property : propertiesInOffers[1]) {
                    transferProperty(tradingPlayers[1], property, tradingPlayers[0]);
                }
                if (cardsInOffers[0] > 0 || cardsInOffers[1] > 0) {
                    for (int j = 0; j < cardsInOffers[0]; j++) {
                        transferCard(tradingPlayers[0], tradingPlayers[1]);
                    }
                    for (int j = 0; j < cardsInOffers[1]; j++) {
                        transferCard(tradingPlayers[1], tradingPlayers[0]);
                    }
                }

            } catch (PlayerBrokeException e) {
                e.printStackTrace();
            } catch (GameEndedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Transfers a property from one player (giver) to another (receiver)
     *
     * @param giver
     * @param property
     * @param receiver
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void transferProperty(Player giver, Property property, Player receiver) {
        property.setOwner(null);
        giver.removeOwnedProperty(property);
        property.setOwner(receiver);
        receiver.addOwnedProperty(property);
    }

    /**
     * Transfers a card from one player (giver) to another (receiver).
     * @param giver
     * @param receiver
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void transferCard(Player giver, Player receiver) {
        ArrayList<Card> giverCards = new ArrayList<>(giver.getOwnedCards());
        ArrayList<Card> receiverCards = new ArrayList<>(receiver.getOwnedCards());

        try {
            Card card = giverCards.get(0);
            giverCards.remove(0);
            receiverCards.add(card);
            giver.setOwnedCards(giverCards);
            receiver.setOwnedCards(receiverCards);
            gui.showMessage(giver + " overfører et benådelseskort til " + receiver + ".");
        } catch (Exception e) {
            System.err.println("Cannot transfer card, since giver owns no cards!");
        }

    }

    /**
     * Shows a menu where the user may choose to mortage properties,
     * if he/she owns any properties, that are not mortgaged.
     * Assures that the player has sold all houses/hotels from real estate, before mortgaging.
     * @param player
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void mortgageAction(Player player) {
        if (player == null) player = choosePlayer("Hvilken spiller ønsker at pantsætte?", null, false);
        if (player == null) return;
        boolean continuePawning = true;
        while (continuePawning) {
            ArrayList<Property> potentialProperties = new ArrayList<>();
            for (Property property : player.getOwnedProperties()) {
                if (!property.getMortgaged()) {
                    potentialProperties.add(property);
                }
            }

            if (potentialProperties.size() == 0) {
                gui.showMessage("Du ejer ingen grunde, du kan pantsætte!");
                return;
            }

            ArrayList<Integer> mortgageValues = new ArrayList<>();
            for (Property p : potentialProperties) mortgageValues.add(p.getCost() / 2);
            Property property = chooseFromOptions(
                    potentialProperties,
                    "Hvilken grund ønsker du at pantsætte?",
                    "Stop med at pantsætte",
                    ", pantsætningsværdi: ",
                    mortgageValues,
                    " kr.");

            if (property == null) {
                return;
            }

            if (property instanceof RealEstate) {
                boolean ableToMortgage = true;
                for (Property p : player.getOwnedProperties()) {
                    if (p.getColorGroup() == property.getColorGroup() && ((RealEstate) p).getHouseCount() > 0) {
                        ableToMortgage = false;
                    }
                }

                if (!ableToMortgage) {
                    gui.showMessage("Du skal sælge alle huse i farvegruppen, før du kan pantsætte!");
                } else {
                    mortgage(player, property);
                }
            } else {
                mortgage(player, property);
            }
        }
    }

    /**
     * Implements the activity where a player mortages a property.
     * @param player
     * @param property
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void mortgage(Player player, Property property) {
        property.setMortgaged(true);
        paymentFromBank(player, property.getCost() / 2);
    }

    /**
     * Shows a menu where a player may choose to "unmortgage" (buy back) a property from the bank,
     * if he/she has any mortgaged properties.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void unmortgageAction() {
        Player player = choosePlayer("Hvilken spiller ønsker at indfri sin gæld i pantsættelser?", null, false);
        if (player == null) return;
        boolean continuePawning = true;
        while (continuePawning) {
            ArrayList<Property> potentialProperties = new ArrayList<>();
            for (Property property : player.getOwnedProperties()) {
                if (property.getMortgaged()) {
                    potentialProperties.add(property);
                }
            }

            if (potentialProperties.size() == 0) {
                gui.showMessage("Du har ingen pantsatte grunde!");
                return;
            }

            ArrayList<Integer> debts = new ArrayList<>();
            for (Property p : potentialProperties) debts.add((int)(p.getCost() / 2 * 1.1));
            Property property = chooseFromOptions(
                    potentialProperties,
                    "Hvilken pantsat grund vil du tilbagebetale?",
                    "Tilbage til hovedmenu",
                    ", pantsætningsgæld: ",
                    debts,
                    " kr.");
            if (property == null) return;
            unmortgage(player, property);
        }
    }

    /**
     * Implements the activity where a player buys a mortgaged property back from the bank.
     * @param player
     * @param property
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private void unmortgage(Player player, Property property) {
        if (player.getBalance() >= property.getCost() / 2 * 1.1) {
            try {
                paymentToBank(player, (int)(property.getCost() / 2 * 1.1));
                property.setMortgaged(false);
            } catch (PlayerBrokeException e) {
                e.printStackTrace();
            } catch (GameEndedException e) {
                e.printStackTrace();
            }
        } else {
            gui.showMessage(player + ", du har ikke råd til at indfri din gæld i denne pantsættelse.");
        }
    }

    /**
     * Use to choose an active player from a dropdown menu in the gui.
     * May exclude a specific player from the list and may exclude players in prison.
     *
     * @param msg            Message over dropdown menu.
     * @param excludedPlayer Exlude a specific player.
     * @param mayBeInPrison  If true, include players in prison. If false, exclude players in prison.
     * @return the chosen player.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private Player choosePlayer(String msg, Player excludedPlayer, boolean mayBeInPrison) {
        //Make list of active players, that either may be or may not be in prison (depending on mayBeInPrison).
        ArrayList<Player> playerList = new ArrayList<>();
        for (Player player : game.getActivePlayers(mayBeInPrison)) {
            if (player != excludedPlayer) playerList.add(player);
        }
        return chooseFromOptions(playerList, msg, "Annuller", null, null, null);
    }

    /**
     * Method with generic return type used to show a dropdown menu in the gui
     * with a certain list of options. The last option in the list is used to
     * cancel and return to menu with no action. Each option may have strings
     * and values added to the end, eg: ", price: 100$/house".
     *
     * @param c          A collection of options.
     * @param msg        Message to be shown over the dropdown menu.
     * @param stopOption The string to represent the option to cancel, eg: "Cancel".
     * @param addToEnd1  String to be added to end of each option, before a certain value.
     * @param values     Values to be added to end of each option.
     * @param addToEnd2  String or Arraylist with values to be added to end of each option, after a certain value.
     *                   If it's a string, the same string will be added to each option.
     *                   If it's an arraylist (of the same size as the collection, c) each element will be added
     *                   to the end of the element of the collection with the same index.
     * @param <T>        Type of the objects listed.
     * @return The chosen option of type T.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    private <T> T chooseFromOptions(Collection<T> c, String msg, String stopOption, String addToEnd1, ArrayList values, Object addToEnd2) {
        String[] options = new String[c.size() + 1];
        Iterator iterator = c.iterator();
        String suffix = "";
        for (int i = 0; i < options.length - 1; i++) {
            if (addToEnd1 == null) addToEnd1 = "";
            if (values != null && addToEnd2 instanceof String) {
                options[i] = iterator.next() + addToEnd1 + values.get(i) + addToEnd2;
            } else if (values != null && addToEnd2 instanceof ArrayList) {
                options[i] = iterator.next() + addToEnd1 + values.get(i) + ((ArrayList) addToEnd2).get(i);
            } else {
                options[i] = iterator.next().toString();
            }

            // String prefix = Integer.toString(i) + ". ";
            suffix += " ";
            options[i] = options[i] + suffix;
        }
        options[options.length - 1] = stopOption;

        String choiceString = gui.getUserSelection(msg, options);
        if (choiceString.equals(stopOption)) return null;
        T choice = null;
        for (T t : c) {
            if (choiceString.contains(t.toString()))
                choice = t;
        }
        return choice;
    }

    /**
     * Used by Space objects to display a message in the gui,
     * instead of letting them access the gui.
     *
     * @param message
     */
    public void showMessage(String message) {
        gui.showMessage(message);
    }

    /**
     * Used by breweries to get the current sum of the dies, used to
     * calculate the rent, when a player lands on the brewery.
     * @return
     */
    public int getSumOfDies() {
        return sumOfDies;
    }

    /**
     * This method is used to play a sound, which is located in the resources/sounds directory.
     * @param fileName filename of the sound.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */
    public void playSound(String fileName) {
        try {
            InputStream is = getClass().getResourceAsStream("/sounds/" + fileName);
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Used by properties and cards to get access to the game object.
     * @return
     */
    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setDiceCup(IDiceCup diceCup) {
        this.diceCup = diceCup;
    }

    /**
     * An implementation of the quickSort-algorithm, used to sort data.
     * @param c Collection of generic type, T. The data to be sorted.
     * @param comparator Comparator. Contains to definition of how to compare the type of data.
     * @param <T> Generic type.
     * @return ArrayList of generic type, T. The sorted list of data.
     * @author This method is inspired by a quicksort-implementation by Ekkart Kindler, ekki@dtu.dk.
     *         It is then changed and further developed upon by Nicolai d T. Wulff,	s185036@student.dtu.dk.
     */
    private <T> ArrayList<T> quickSort(Collection<T> c, Comparator<T> comparator) {
        class QuickSorter {
            private void sort(ArrayList<T> arr, int lower, int upper) {
                if (lower < upper) {
                    int i = lower, j = upper;
                    T pivot = arr.get((i + j) / 2);
                    do {
                        while (comparator.compare(arr.get(i), pivot) < 0) i++;
                        while (comparator.compare(pivot, arr.get(j)) < 0) j--;
                        if (i <= j) {
                            T temp = arr.get(i);
                            arr.set(i, arr.get(j));
                            arr.set(j, temp);
                            i++;
                            j--;
                        }
                    } while (i <= j);
                    sort(arr, lower, j);
                    sort(arr, i, upper);
                }
            }
        }
        QuickSorter qs = new QuickSorter();
        ArrayList<T> arr = new ArrayList<>(c);
        qs.sort(arr, 0, arr.size() - 1);
        return arr;
    }

    /**
     * This method is yet to be finished or utilized in the code.
     * The method could be further developed into a feature, where
     * the user could choose to display various sorted information about the
     * state of the game somewhere in the GUI, eg:
     *      1. How the players rank according to their total assets.
     *      2. How all properties rank according to their current rent.
     *      3. Or something else.
     * Different comparators should be defined in this method.
     * This method uses the quickSort-method (see above), which is passed a
     * collection and a comparator, and then returns a sorted arrayList.
     * @author Nicolai d T. Wulff,	s185036@student.dtu.dk
     */

    public void displaySortedInfo() {

        //TODO: Display a menu with options to display different sorted information.

        Comparator totalAssetsComparator = new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                return Integer.compare(p1.calculateTotalAssets(false), p2.calculateTotalAssets(false));
            }
        };

        Comparator rentComparator = new Comparator<Property>() {
            @Override
            public int compare(Property p1, Property p2) {
                return Integer.compare(p1.getRent(), p2.getRent());
            }
        };

        //Todo: add more comparators. Use quickSort to sort some data, and finally display it.

        //Example of how to use the quicksort-method. Here, it prints a sorted list to the console:
        for (Object p : quickSort(game.getPlayers(), totalAssetsComparator)) {
            System.out.println(p.toString());
        }
    }
}