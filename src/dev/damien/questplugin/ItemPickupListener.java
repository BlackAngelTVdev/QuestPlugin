package dev.damien.questplugin;
import dev.damien.questplugin.QuestManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;


import java.util.UUID;

public class ItemPickupListener implements Listener {

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == QuestManager.getCurrentMaterial()) {
            UUID playerId = event.getPlayer().getUniqueId();
            QuestManager.addProgress(playerId, item.getAmount());
        }
    }
}
