package dev.damien.questplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.*;
import java.util.*;

public class QuestManager {

    private static int currentQuestId = -1;
    private static Material currentMaterial;
    private static int requiredAmount;
    private static final Map<UUID, Integer> playerProgress = new HashMap<>();
    private static final Set<UUID> completedPlayers = new HashSet<>();
    private static final Random random = new Random();

    // ===============================
    // ⚙️ Chargement de la quête active
    // ===============================
    public static void loadQuestFromDB() {
        try (Connection conn = MySQLManager.getNewConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM current_quest ORDER BY id DESC LIMIT 1")) {

            if (rs.next()) {
                currentQuestId = rs.getInt("id");
                currentMaterial = Material.valueOf(rs.getString("material"));
                requiredAmount = rs.getInt("required_amount");



                // Recharge la progression des joueurs depuis la db
                loadAllPlayerProgress(conn);

                return;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Erreur en chargeant la quête depuis MySQL : " + e.getMessage());
        }

        loadNewQuest();
    }

    // ===============================
    // 🔹 Charge la progression de tous les joueurs
    // ===============================
    private static void loadAllPlayerProgress(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT uuid, progress FROM quest_progress WHERE quest_id = ?"
        )) {
            ps.setInt(1, currentQuestId);
            ResultSet rs = ps.executeQuery();

            playerProgress.clear();
            completedPlayers.clear();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int progress = rs.getInt("progress");
                playerProgress.put(uuid, progress);
                if (progress >= requiredAmount) completedPlayers.add(uuid);
            }

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Erreur en chargeant les progressions : " + e.getMessage());
        }
    }

    // ===============================
    // 🆕 Nouvelle quête
    // ===============================
    public static void loadNewQuest() {
        try (Connection conn = MySQLManager.getNewConnection()) {
            List<String> materials = Main.getInstance().getConfig().getStringList("possible-items");
            if (materials.isEmpty()) return;

            String matName = materials.get(random.nextInt(materials.size()));
            currentMaterial = Material.valueOf(matName);

            int min = Main.getInstance().getConfig().getInt("quantity-range.min");
            int max = Main.getInstance().getConfig().getInt("quantity-range.max");
            requiredAmount = random.nextInt(max - min + 1) + min;

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO current_quest (material, required_amount) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, currentMaterial.name());
            ps.setInt(2, requiredAmount);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) currentQuestId = keys.getInt(1);

            playerProgress.clear();
            completedPlayers.clear();

            Bukkit.getLogger().info("💾 Nouvelle quête sauvegardée en MySQL : " + currentMaterial + " x" + requiredAmount);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Erreur MySQL (loadNewQuest) : " + e.getMessage());
        }
    }

    // ===============================
    // 🔹 Joueur rejoint le serveur
    // ===============================
    public static void playerJoin(UUID uuid) {
        if (currentQuestId == -1) loadQuestFromDB();

        try (Connection conn = MySQLManager.getNewConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT progress FROM quest_progress WHERE quest_id = ? AND uuid = ?"
             )) {
            ps.setInt(1, currentQuestId);
            ps.setString(2, uuid.toString());
            ResultSet rs = ps.executeQuery();

            int progress = 0;
            if (rs.next()) progress = rs.getInt("progress");

            playerProgress.put(uuid, progress);
            if (progress >= requiredAmount) completedPlayers.add(uuid);

        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Erreur MySQL (playerJoin) : " + e.getMessage());
        }
    }

    // ===============================
    // ⛏️ Ajout de progression
    // ===============================
    public static void addProgress(UUID uuid, int amount) {
        if (completedPlayers.contains(uuid)) return;

        int total = Math.min(playerProgress.getOrDefault(uuid, 0) + amount, requiredAmount);
        playerProgress.put(uuid, total);
        savePlayerProgress(uuid);

        if (total >= requiredAmount) {
            completedPlayers.add(uuid);
            completeQuest(uuid);
        }

        updateQuestItem(uuid);
    }

    // ===============================
    // 💾 Sauvegarde progression
    // ===============================
    public static void savePlayerProgress(UUID uuid) {
        try (Connection conn = MySQLManager.getNewConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "REPLACE INTO quest_progress (quest_id, uuid, progress) VALUES (?, ?, ?)"
             )) {
            ps.setInt(1, currentQuestId);
            ps.setString(2, uuid.toString());
            ps.setInt(3, playerProgress.getOrDefault(uuid, 0));
            ps.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("❌ Erreur MySQL (savePlayerProgress) : " + e.getMessage());
        }
    }

    // ===============================
    // 🏁 Fin de quête
    // ===============================
    public static void completeQuest(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) player.sendMessage("🏆 Vous avez complété la quête !");

        OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(uuid);
        String playerName = offPlayer.getName() != null ? offPlayer.getName() : "Joueur";

        int montant = Main.getInstance().getConfig().getInt("quest.montant", 0);
        String rawCommand = Main.getInstance().getConfig().getString("quest.complete-command");

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (montant > 0)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + playerName + " " + montant);
            if (rawCommand != null && !rawCommand.isEmpty())
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rawCommand.replace("[player]", playerName));
        });

        Bukkit.getLogger().info("🏆 " + playerName + " a complété la quête !");
    }

    // ===============================
    // 🪟 GUI / Item
    // ===============================
    public static void updateQuestItem(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != currentMaterial) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName("§bProgression: " + playerProgress.getOrDefault(uuid, 0) + "/" + requiredAmount);
        meta.setLore(Arrays.asList(
                "§7Collecte §b" + requiredAmount + " §7de §3" + currentMaterial.name()
        ));
        item.setItemMeta(meta);
    }

    public static void openQuestGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "§aObjectif : §3" + currentMaterial.name());

        ItemStack questItem = new ItemStack(currentMaterial);
        ItemMeta meta = questItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bProgression: " + playerProgress.getOrDefault(player.getUniqueId(), 0) + "/" + requiredAmount);
            meta.setLore(Arrays.asList(
                    "§7Collecte §b" + requiredAmount + " §7de §3" + currentMaterial.name(),
                    "",
                    "§8Les items sont comptés quand ils sont ramassés"
            ));
            questItem.setItemMeta(meta);
        }
        gui.setItem(3, questItem);

        ItemStack topItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) topItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setDisplayName("§6Top 5 des collecteurs");

            List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(playerProgress.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());

            List<String> lore = new ArrayList<>();
            int rank = 1;
            for (Map.Entry<UUID, Integer> e : sorted) {
                if (rank > 5) break;
                OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(e.getKey());
                lore.add("§7#" + rank + " §f" + (topPlayer.getName() != null ? topPlayer.getName() : "Joueur") + " §8- §b" + e.getValue());

                // Set head du leader
                if (rank == 1) skullMeta.setOwningPlayer(topPlayer);
                rank++;
            }

            if (lore.isEmpty()) lore.add("§8Aucun joueur");
            skullMeta.setLore(lore);
            topItem.setItemMeta(skullMeta);
        }
        gui.setItem(5, topItem);
        player.openInventory(gui);
    }

    // ===============================
    // 🔁 Refresh auto quête toutes les 10 secondes
    // ===============================
    public static void startAutoRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(),
                () -> {
                    Material oldMaterial = currentMaterial;
                    int oldAmount = requiredAmount;

                    loadQuestFromDB();

                    // Si le material ou la quantité a changé, met à jour tous les joueurs
                    if (!Objects.equals(oldMaterial, currentMaterial) || oldAmount != requiredAmount) {
                        for (Player p : Bukkit.getOnlinePlayers()) updateQuestItem(p.getUniqueId());
                    }
                }, 20L * 10, 20L * 10); // toutes les 10 sec
    }

    public static void startPlayerProgressRefresh() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(),
                () -> {
                    try (Connection conn = MySQLManager.getNewConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT uuid, progress FROM quest_progress WHERE quest_id = ?"
                         )) {
                        ps.setInt(1, currentQuestId);
                        ResultSet rs = ps.executeQuery();

                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            int progress = rs.getInt("progress");

                            int oldProgress = playerProgress.getOrDefault(uuid, 0);
                            if (progress != oldProgress) {
                                playerProgress.put(uuid, progress);
                                if (progress >= requiredAmount) completedPlayers.add(uuid);
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) updateQuestItem(uuid);
                            }
                        }
                    } catch (SQLException ignored) {}
                }, 20L * 5, 20L * 5); // toutes les 5 sec
    }
    // ===============================
    // 🔹 Getters
    // ===============================
    public static Material getCurrentMaterial() { return currentMaterial; }
    public static int getRequiredAmount() { return requiredAmount; }
    public static Map<UUID, Integer> getPlayerProgress() { return playerProgress; }
    public static Set<UUID> getCompletedPlayers() { return completedPlayers; }
}
