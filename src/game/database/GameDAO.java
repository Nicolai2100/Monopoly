package game.database;

import game.model.Game;
import game.model.Player;
import game.model.Property;
import game.model.Space;
import game.model.properties.RealEstate;
import game.model.properties.Utility;

import java.awt.*;
import java.sql.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeppe s170196, Nicolai s185036, Nicolai Larsen s185020
 */
public class GameDAO implements IGameDAO {
    private static final String url = "jdbc:mysql://ec2-52-30-211-3.eu-west-1.compute.amazonaws.com/s185020";
    private static final String user = "s185020";
    private static final String password = "iEFSqK2BFP60YWMPlw77I";
    private static Connection connection;

    public GameDAO() {
        initializeDataBase();
    }

    /**
     * Metoden opretter en forbindelse til databasen, som gemmes som i en lokal variabel.
     *
     * @return
     * @throws SQLException
     * @author Jeppe s170196
     * @author Nicolai J. Larsen
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Metoden bruges til at gemme et spil i databasen
     *
     * @author Jeppe s170196
     * @author Nicolai J. Larsen
     */
    @Override
    public void saveGame(Game game) {
        long performance = System.currentTimeMillis();
        try {
            getConnection().setAutoCommit(false);
            PreparedStatement insertGame = getConnection().prepareStatement(
                    "INSERT INTO game (curplayerid, date) " +
                            "VALUES(?,?);", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement insertPLayers = getConnection().prepareStatement(
                    "INSERT INTO player " +
                            "VALUES(?,?,?,?,?,?,?,?,?);");

            PreparedStatement insertProperties = getConnection().prepareStatement(
                    "INSERT INTO property " +
                            "VALUES(?,?,?,?,?,?);");
            int curPlayer = game.getPlayers().indexOf(game.getCurrentPlayer());
            insertGame.setInt(1, curPlayer);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            insertGame.setString(2, dtf.format(now));

            insertGame.executeUpdate();
            ResultSet gen = insertGame.getGeneratedKeys();
            int gameid = 0;
            if (gen.next()) {
                gameid = gen.getInt(1);
                game.setGameId(gameid);
            }

            int index = 0;
            for (Player player : game.getPlayers()) {

                index = game.getPlayers().indexOf(player);
                insertPLayers.setInt(1, index);
                insertPLayers.setString(2, player.getName());
                insertPLayers.setInt(3, player.getBalance());
                insertPLayers.setInt(4, player.getCurrentPosition().getIndex());
                insertPLayers.setBoolean(5, player.isInPrison());
                insertPLayers.setBoolean(6, player.isBroke());
                insertPLayers.setInt(7, gameid);
                insertPLayers.setInt(8, player.getColor().getRGB());
                insertPLayers.setString(9, player.getCarType().toString());
                insertPLayers.executeUpdate();
                insertPLayers.clearParameters();

                for (Space space : player.getOwnedProperties()) {
                    if (space instanceof Property) {
                        insertProperties.setInt(1, space.getIndex());
                        if (space instanceof RealEstate) {
                            RealEstate realEstate = (RealEstate) space;
                            insertProperties.setInt(2, realEstate.getHouseCount());
                            insertProperties.setBoolean(3, realEstate.getSuperOwned());
                            insertProperties.setString(6, "realestate");
                        }
                        if (space instanceof Utility) {
                            Utility utility = (Utility) space;
                            insertProperties.setInt(2, 0);
                            insertProperties.setBoolean(3, utility.getSuperOwned());
                            insertProperties.setString(6, "utility");
                        }

                        int playerId = game.getPlayers().indexOf(player);
                        insertProperties.setInt(4, playerId);
                        insertProperties.setInt(5, gameid);
                        insertProperties.executeUpdate();
                        insertProperties.clearParameters();
                    }
                }
            }
            getConnection().commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Tid det tog at gemme spillet i databasen: " + (System.currentTimeMillis() - performance) + "ms");
    }

    /**
     * Metoden "opdaterer" et allerede gemt spil.
     *
     * @author Jeppe s170196
     */
    @Override
    public void updateGame(Game game) {
        deleteGame(game);
        saveGame(game);
    }

    /**
     * Metoden bruges til at slette et spil fra databasen.
     * @author Jeppe s170196
     */
    @Override
    public void deleteGame(Game game) {
        try {
            PreparedStatement gameStm = getConnection().prepareStatement("DELETE FROM game WHERE gameid = ?");
            gameStm.setInt(1, game.getGameId());
            gameStm.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metoden bruges til at hente et gemt spil fra databasen
     *
     * @param game
     * @return a game
     * @author Jeppe s170196
     */
    @Override
    public Game loadGame(Game game, String dateOfGameToLoad) {
        long performance = System.currentTimeMillis();
        try {
            PreparedStatement playerStm = getConnection().prepareStatement("SELECT * FROM game NATURAL JOIN player WHERE date=?");
            PreparedStatement propertyStm = getConnection().prepareStatement("SELECT * FROM game NATURAL JOIN property WHERE date=?");

            playerStm.setString(1, dateOfGameToLoad);
            propertyStm.setString(1, dateOfGameToLoad);

            ResultSet playerRS = playerStm.executeQuery();
            ResultSet propertyRS = propertyStm.executeQuery();

            int curplayerid = 0;
            List<Player> listOfPlayers = new ArrayList<Player>();
            List<Space> listOfSpaces = new ArrayList<Space>();

            while (playerRS.next()) {
                game.setGameId(playerRS.getInt("gameid"));
                curplayerid = playerRS.getInt("curplayerid");

                Player p = new Player();
                p.setName(playerRS.getString("name"));
                p.setCurrentPosition(game.getSpaces().get(playerRS.getInt("position")));
                p.setBalance(playerRS.getInt("balance"));
                p.setInPrison(playerRS.getBoolean("injail"));
                p.setBroke(playerRS.getBoolean("isbroke"));
                p.setColor(new Color(playerRS.getInt("color")));
                p.setCarType(Player.CarType.getCarTypeFromString(playerRS.getString("token")));
                listOfPlayers.add(playerRS.getInt("playerid"), p);
            }
            game.setPlayers(listOfPlayers);
            game.setCurrentPlayer(game.getPlayers().get(curplayerid));

            listOfSpaces.addAll(game.getSpaces());
            while (propertyRS.next()) {
                if (propertyRS.getInt("playerid") != -1) {
                    if (propertyRS.getString("type").equals("utility")) {
                        Utility utility = (Utility) listOfSpaces.get(propertyRS.getInt("posonboard"));

                        utility.setSuperOwned(propertyRS.getBoolean("superowned"));
                        utility.setOwner(game.getPlayers().get(propertyRS.getInt("playerid")));
                        utility.getOwner().addOwnedProperty(utility);

                        listOfSpaces.set(propertyRS.getInt("posonboard"), utility);
                    } else if (propertyRS.getString("type").equals("realestate")) {
                        RealEstate realEstate = (RealEstate) listOfSpaces.get(propertyRS.getInt("posonboard"));

                        realEstate.setSuperOwned(propertyRS.getBoolean("superowned"));
                        realEstate.setOwner(game.getPlayers().get(propertyRS.getInt("playerid")));
                        realEstate.getOwner().addOwnedProperty(realEstate);
                        realEstate.setHouseCount(propertyRS.getInt("numofhouses"));

                        listOfSpaces.set(propertyRS.getInt("posonboard"), realEstate);
                    }
                }
            }
            game.setSpaces(listOfSpaces);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Tid det tog at hente spillet i databasen:" + (System.currentTimeMillis() - performance) + "ms");
        return game;
    }

    /**
     * @author Jeppe s170196
     */
    @Override
    public List<String> getGamesList() {

        List gameList = new ArrayList<String>();

        try {
            PreparedStatement gameStm = getConnection().prepareStatement("SELECT * FROM game ORDER BY gameid DESC");
            ResultSet gameRS = gameStm.executeQuery();
            while (gameRS.next()) {
                gameList.add(gameRS.getString("date"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gameList;
    }

    /**
     * Metoden sætter alle tabellerne op, hvis de ikke allerede ligger i databasen.
     * @author Nicolai J. Larsen
     */
    public void initializeDataBase() {
        try {
            getConnection().setAutoCommit(false);
            PreparedStatement createTableGame = getConnection().prepareStatement(
                    "CREATE TABLE if NOT EXISTS game " +
                            "(gameid int NOT NULL AUTO_INCREMENT, " +
                            "date VARCHAR(20) NOT NULL, " +
                            "curplayerid int, " +
                            "primary key (gameid));");

            PreparedStatement createTablePlayer = getConnection().prepareStatement(
                    "CREATE TABLE if NOT EXISTS player " +
                            "(playerid int, " +
                            "name varchar(20), " +
                            "balance int, " +
                            "position int, " +
                            "injail bit, " +
                            "isbroke bit, " +
                            "gameid int, " +
                            "color int, " +
                            "token varchar(10)," +
                            "primary key (playerid, gameid), " +
                            "FOREIGN KEY (gameid) REFERENCES game (gameid) " +
                            "ON DELETE CASCADE);");

            PreparedStatement createTableProperty = getConnection().prepareStatement(
                    "CREATE TABLE if NOT EXISTS property " +
                            "(posonboard int, " +
                            "numofhouses int, " +
                            "superowned bit, " +
                            "playerid int, " +
                            "gameid int, " +
                            "type varchar(20), " +
                            "primary key (posonboard, gameid), " +
                            "FOREIGN KEY (gameid) REFERENCES game (gameid) " +
                            "ON DELETE CASCADE);");

            createTableGame.execute();
            createTablePlayer.execute();
            createTableProperty.execute();
            getConnection().commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
