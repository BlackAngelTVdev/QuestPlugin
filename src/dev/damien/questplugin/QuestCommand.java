package dev.damien.questplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.ConsoleCommandSender;

public class QuestCommand implements CommandExecutor {

    private final QuestManager questManager;

    // Constructeur de la classe QuestCommand pour initialiser QuestManager
    public QuestCommand(QuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Si la commande provient d'un joueur
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Si la commande est "/quest", on ouvre le GUI de la quête
            if (command.getName().equalsIgnoreCase("quest")) {
                questManager.openQuestGUI(player); // On appelle la méthode pour afficher le GUI de la quête
                return true;
            }
        } else if (sender instanceof ConsoleCommandSender) {
            // Si la commande provient de la console, on gère la commande "/nextquest"
            if (command.getName().equalsIgnoreCase("nextquest")) {
                // Vérifie si l'expéditeur a la permission d'exécuter la commande
                if (sender.hasPermission("questplugin.nextquest")) {
                    questManager.loadNewQuest(); // Charge une nouvelle quête
                    sender.sendMessage("La prochaine quête a été générée !");
                    return true;
                } else {
                    sender.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
                    return false;
                }
            }
        }
        return false; // Retourne false si aucune commande n'est reconnue
    }
}
