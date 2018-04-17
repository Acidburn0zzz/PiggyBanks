package pl.plajer.piggybanks;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.VisibilityManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.plajer.piggybanks.utils.Utils;

import java.util.List;

public class Commands implements CommandExecutor {

    private Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
        plugin.getCommand("piggybanks").setExecutor(this);
    }

    private boolean isSenderPlayer(CommandSender sender) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Only-Player"));
            return false;
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if(!sender.hasPermission(permission)) {
            sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.No-Permission"));
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("piggybanks")) {
            if(args.length == 0) {
                sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Help-Command.Header"));
                sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Help-Command.Description"));
                return true;
            }
            if(args[0].equalsIgnoreCase("create")) {
                if(!isSenderPlayer(sender)) return true;
                if(!hasPermission(sender, "piggybanks.admin.create")) return true;
                if(args.length == 1) {
                    sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Pig-Type"));
                    return true;
                }
                if(!(args[1].equalsIgnoreCase("adult") || args[1].equalsIgnoreCase("baby"))) {
                    sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Pig-Type"));
                    return true;
                }
                Player p = (Player) sender;
                Pig pig = (Pig) p.getWorld().spawnEntity(p.getLocation(), EntityType.PIG);
                pig.setAI(false);
                pig.setCollidable(false);
                if(args[1].equalsIgnoreCase("adult")) {
                    pig.setAdult();
                } else {
                    pig.setBaby();
                }
                pig.setAgeLock(true);
                List<String> list = plugin.getFileManager().getPiggyBanksConfig().getStringList("piggybanks");
                list.add(pig.getUniqueId().toString());
                plugin.getFileManager().getPiggyBanksConfig().set("piggybanks", list);
                plugin.getFileManager().savePiggyBanksConfig();
                Hologram hologram = HologramsAPI.createHologram(plugin, pig.getLocation().clone().add(0, 2.2, 0));
                if(plugin.isEnabled("ProtocolLib")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if(hologram.isDeleted()) this.cancel();
                            VisibilityManager vm = hologram.getVisibilityManager();
                            for(Player player : Bukkit.getOnlinePlayers()) {
                                if(hologram.isDeleted()) this.cancel();
                                vm.showTo(player);
                                vm.setVisibleByDefault(false);
                                hologram.removeLine(0);
                                hologram.insertTextLine(0, Utils.colorFileMessage("PiggyBank.Pig.Name-With-Counter").replaceAll("%money%", plugin.getFileManager().getUsersConfig().get("users." + player.getUniqueId()).toString()));
                            }
                        }
                    }.runTaskTimer(plugin, 10, 10);
                }
                hologram.appendTextLine(Utils.colorFileMessage("PiggyBank.Pig.Name"));
                for(String s : Utils.colorFileMessage("PiggyBank.Pig.Name-Description").split(";")) {
                    hologram.appendTextLine(s);
                }
                sender.sendMessage(Utils.colorFileMessage("PiggyBank.Pig.Created-Successfully"));
                List<PiggyBank> pigBanks = plugin.getPiggyManager().getLoadedPiggyBanks();
                pigBanks.add(new PiggyBank(pig, pig.getLocation(), hologram));
                plugin.getPiggyManager().setLoadedPiggyBanks(pigBanks);
                return true;
            }
            if(args[0].equalsIgnoreCase("remove")) {
                if(!isSenderPlayer(sender)) return true;
                if(!hasPermission(sender, "piggybanks.admin.remove")) return true;
                Entity target = Utils.getTargetEntity((Player) sender);
                if(target == null) {
                    sender.sendMessage(Utils.colorFileMessage("PiggyBank.Pig.Target-Invalid"));
                    return true;
                } else {
                    if(!target.getType().equals(EntityType.PIG)) {
                        sender.sendMessage(Utils.colorFileMessage("PiggyBank.Pig.Target-Invalid"));
                        return true;
                    }
                    List<String> list = plugin.getFileManager().getPiggyBanksConfig().getStringList("piggybanks");
                    if(!list.contains(target.getUniqueId().toString())) {
                        sender.sendMessage(Utils.colorFileMessage("PiggyBank.Pig.Target-Invalid"));
                        return true;
                    }
                    list.remove(target.getUniqueId().toString());
                    plugin.getFileManager().getPiggyBanksConfig().set("piggybanks", list);
                    plugin.getFileManager().savePiggyBanksConfig();
                    for(PiggyBank pgb : plugin.getPiggyManager().getLoadedPiggyBanks()) {
                        if(pgb.getPiggyBankEntity().equals(target)) {
                            pgb.getPiggyBankEntity().remove();
                            pgb.getPiggyHologram().delete();
                            plugin.getPiggyManager().getLoadedPiggyBanks().remove(pgb);
                            sender.sendMessage(Utils.colorFileMessage("PiggyBank.Pig.Removed"));
                            return true;
                        }
                    }
                    return true;
                }
            }
            if(args[0].equalsIgnoreCase("list")) {
                if(!hasPermission(sender, "piggybanks.admin.list")) return true;
                sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.Loaded-Piggies"));
                int i = 0;
                for(PiggyBank pgb : plugin.getPiggyManager().getLoadedPiggyBanks()) {
                    sender.sendMessage(ChatColor.GOLD + "x: " + pgb.getPigLocation().getBlockX() + " y: " + pgb.getPigLocation().getBlockY() + " z: " + pgb.getPigLocation().getBlockZ());
                    i++;
                }
                if(i == 0) sender.sendMessage(Utils.colorFileMessage("PiggyBank.Command.No-Loaded-Piggies"));
                return true;
            }
        }
        return false;
    }


}
