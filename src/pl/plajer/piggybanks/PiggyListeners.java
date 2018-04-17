package pl.plajer.piggybanks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.plajer.piggybanks.utils.UpdateChecker;
import pl.plajer.piggybanks.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PiggyListeners implements Listener {

    private List<Integer> piggyValues = Arrays.asList(1, 5, 10, 25, 50, 100, -1);
    private Main plugin;
    @Getter
    private HashMap<Player, Pig> openedPiggies = new HashMap<>();

    PiggyListeners(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onPiggyBankDamage(EntityDamageEvent e) {
        if(e.getEntity() instanceof Pig) {
            for(PiggyBank pgb : plugin.getPiggyManager().getLoadedPiggyBanks()) {
                if(pgb.getPiggyBankEntity().equals(e.getEntity())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPiggyBankDamage(EntityDamageByEntityEvent e) {
        if(e.getEntity() instanceof Pig && e.getDamager() instanceof Player) {
            Player attacker = (Player) e.getDamager();
            Pig piggy = (Pig) e.getEntity();
            for(PiggyBank pgb : plugin.getPiggyManager().getLoadedPiggyBanks()) {
                if(pgb.getPiggyBankEntity().equals(piggy)) {
                    e.setCancelled(true);
                    if(!e.getDamager().hasPermission("piggybanks.use")) {
                        e.getDamager().sendMessage(Utils.colorFileMessage("PiggyBank.Pig.No-Permission"));
                        return;
                    }
                    openedPiggies.put(attacker, piggy);
                    Inventory piggyBank = Bukkit.createInventory(null, 3 * 9, Utils.colorFileMessage("PiggyBank.Menu.Title"));
                    if(plugin.getConfig().getBoolean("enderchest-enabled")) {
                        piggyBank = Bukkit.createInventory(null, 5 * 9, Utils.colorFileMessage("PiggyBank.Menu.Title"));
                        ItemStack ender = new ItemStack(Material.ENDER_CHEST, 1);
                        ItemMeta enderMeta = ender.getItemMeta();
                        enderMeta.setDisplayName(Utils.colorFileMessage("PiggyBank.Menu.Enderchest-Name"));
                        enderMeta.setLore(Collections.singletonList(Utils.colorFileMessage("PiggyBank.Menu.Enderchest-Lore")));
                        ender.setItemMeta(enderMeta);
                        piggyBank.setItem(31, ender);
                    }
                    ItemStack balance = new ItemStack(Material.BOOK, 1);
                    ItemMeta balanceMeta = balance.getItemMeta();
                    balanceMeta.setDisplayName(Utils.colorFileMessage("PiggyBank.Menu.Balance-Icon").replaceAll("%coins%", String.valueOf(plugin.getFileManager().getUsersConfig().get("users." + e.getDamager().getUniqueId()))).replaceAll("null", "0"));
                    balance.setItemMeta(balanceMeta);
                    piggyBank.setItem(4, balance);
                    for(int i = 0; i < 7; i++) {
                        ItemStack itemStack;
                        if(i == 0) {
                            itemStack = new ItemStack(Material.GOLD_NUGGET, 1);
                        } else if(i >= 1 && i <= 4) {
                            itemStack = new ItemStack(Material.GOLD_INGOT, 1);
                        } else {
                            itemStack = new ItemStack(Material.GOLD_BLOCK, 1);
                        }
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(Utils.colorFileMessage("PiggyBank.Menu.Withdraw-Format").replaceAll("%coins%", String.valueOf(piggyValues.get(i)).replaceAll("-1", "all")));
                        itemMeta.setLore(Collections.singletonList(Utils.colorFileMessage("PiggyBank.Menu.Withdraw-Lore").replaceAll("%coins%", String.valueOf(piggyValues.get(i)).replaceAll("-1", "all"))));
                        itemStack.setItemMeta(itemMeta);
                        piggyBank.setItem(i + 10, itemStack);
                    }
                    attacker.openInventory(piggyBank);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPiggyBankClick(PlayerInteractEntityEvent e) {
        //To avoid event called twice
        if(e.getHand() == EquipmentSlot.OFF_HAND) return;
        if(e.getRightClicked().getType().equals(EntityType.PIG)) {
            for(PiggyBank pgb : plugin.getPiggyManager().getLoadedPiggyBanks()) {
                if(pgb.getPiggyBankEntity().equals(e.getRightClicked())) {
                    Integer money = 1;
                    if(e.getPlayer().isSneaking()) {
                        money = 10;
                    }
                    if(plugin.getEconomy().getBalance(e.getPlayer()) >= money) {
                        plugin.getEconomy().withdrawPlayer(e.getPlayer(), money);
                        e.getRightClicked().getWorld().playEffect(e.getRightClicked().getLocation(), Effect.LAVA_POP, 1);
                        plugin.getFileManager().getUsersConfig().set("users." + e.getPlayer().getUniqueId(), plugin.getFileManager().getUsersConfig().getInt("users." + e.getPlayer().getUniqueId()) + money);
                        plugin.getFileManager().saveUsersConfig();
                        e.getPlayer().sendMessage(Utils.colorFileMessage("PiggyBank.Money-Deposited").replaceAll("%coins%", String.valueOf(money)));
                    } else {
                        e.getPlayer().sendMessage(Utils.colorFileMessage("PiggyBank.Money-No-Money-Balance"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(!plugin.getFileManager().getUsersConfig().isSet("users." + e.getPlayer().getUniqueId())) {
            plugin.getFileManager().getUsersConfig().set("users." + e.getPlayer().getUniqueId(), 0);
            plugin.getFileManager().saveUsersConfig();
        }
        if(e.getPlayer().hasPermission("piggybabnks.admin.notify")) {
            String currentVersion = "v" + Bukkit.getPluginManager().getPlugin("PiggyBanks").getDescription().getVersion();
            if(plugin.getConfig().getBoolean("update-notify")) {
                try {
                    UpdateChecker.checkUpdate(currentVersion);
                    String latestVersion = UpdateChecker.getLatestVersion();
                    if(latestVersion != null) {
                        latestVersion = "v" + latestVersion;
                        e.getPlayer().sendMessage(Utils.colorFileMessage("Other.Plugin-Up-To-Date").replaceAll("%old%", currentVersion).replaceAll("%new%", latestVersion));
                    }
                } catch(Exception ex) {
                    Bukkit.getConsoleSender().sendMessage(Utils.colorFileMessage("Other.Plugin-Update-Check-Failed"));
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        openedPiggies.remove(e.getPlayer());
    }

}
