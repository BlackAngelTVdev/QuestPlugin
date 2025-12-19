package dev.damien.questplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NextQuestCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cTu dois être opérateur pour utiliser cette commande.");
            return true;
        }

        QuestManager.loadNewQuest();
        sender.sendMessage("§aNouvelle quête générée !");
        return true;
    }
}
