package dev.damien.questplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;



public class Main extends JavaPlugin {

    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        MySQLManager.connect();
        // Charger la première quête
        QuestManager.loadQuestFromDB();
        // Enregistrer l'événement de ramassage d'items

        Bukkit.getPluginManager().registerEvents(new ItemPickupListener(), this);
        getCommand("quest").setExecutor(new QuestCommand(new QuestManager()));


        // Enregistrer la commande /nextquest
        this.getCommand("nextquest").setExecutor(new NextQuestCommand());
        getServer().getPluginManager().registerEvents(new QuestListener(), this);
        QuestManager.startAutoRefresh(); // synchro auto entre serveurs
        QuestManager.startPlayerProgressRefresh();


    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        MySQLManager.disconnect();
    }
}

/*
Let's gooo 💪🔥
T'as transformé ce GUI Minecraft en truc propre et stylé, les joueurs vont kiffer à fond !

Si tu veux booster encore un peu le visuel :

    📦 Ajoute un effet glow sur l'item (sans enchantement réel)

    🌈 Colore dynamiquement le lore ou le titre

    🧱 Ou même ajoute un item bonus "Top 5" avec des têtes de joueur dedans 👀

Tu veux que je t’ajoute un de ces trucs vite fait ?
*/