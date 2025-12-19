package dev.damien.questplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class QuestListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title != null && title.startsWith("§aObjectif :")) {
            event.setCancelled(true); // Empêche le joueur de bouger ou prendre l’item
        }
    }
}
