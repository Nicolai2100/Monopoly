package dk.dtu.compute.se.pisd.monopoly.mini.database;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

public class GameDAOTest {
    GameDAO gameDAO;
    Connection conn;

    @Before
    public void initialize() throws SQLException {
        gameDAO = new GameDAO();
        conn = gameDAO.createConnection();
    }

    @Test
    public void connection() throws SQLException {
        assertTrue(!conn.isClosed());
/*
        assertTrue(gameDAO.createConnection().isClosed());
*/
        }


    @Test
    public void saveGame() {
    }

    @Test
    public void loadGame() {
    }

    @Test
    public void getGamesList() {
    }

    @Test
    public void updateGame() {
    }

    @Test
    public void deleteGame() {
    }
}