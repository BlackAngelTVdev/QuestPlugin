package dev.damien.questplugin;

import org.bukkit.Bukkit;

import java.sql.*;

public class MySQLManager {
    private static final String HOST = "mc-mysql";
    private static final String DATABASE = "quest";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private static Connection connection;

    public static void connect() {
        try {
            String host = Main.getInstance().getConfig().getString("mysql.host");
            String user = Main.getInstance().getConfig().getString("mysql.user");
            String password = Main.getInstance().getConfig().getString("mysql.password");
            String database = Main.getInstance().getConfig().getString("mysql.database");

            String url = "jdbc:mysql://" + host + ":3306/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, user, password);

            Bukkit.getLogger().info("✅ Connexion MySQL réussie !");
            createTables();
        } catch (Exception e) {
            Bukkit.getLogger().severe("❌ Erreur de connexion MySQL : " + e.getMessage());
        }
    }

    public static Connection getNewConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://" + HOST + "/" + DATABASE + "?useSSL=false&autoReconnect=true",
                USER,
                PASSWORD
        );
    }
    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                Bukkit.getLogger().info("🔌 Déconnexion MySQL effectuée.");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erreur lors de la déconnexion MySQL : " + e.getMessage());
        }
    }

    private static void createTables() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_progress (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "progress INT NOT NULL DEFAULT 0," +
                    "quest_id VARCHAR(64) NOT NULL," +
                    "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ");");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS current_quest (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "material VARCHAR(64) NOT NULL," +
                    "required_amount INT NOT NULL," +
                    "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            Main.getInstance().getLogger().info("🧩 Tables 'player_progress' et 'current_quest' vérifiées/créées.");
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("❌ Erreur lors de la création des tables : " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = getNewConnection();
        }
        return connection;
    }
}
