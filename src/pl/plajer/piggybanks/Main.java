/*
 * PiggyBanks - Simple piggies for your server
 * Copyright (C) 2018  Plajer's Lair - maintained by Plajer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.plajer.piggybanks;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import pl.plajer.piggybanks.piggy.PiggyBank;
import pl.plajer.piggybanks.piggy.PiggyListeners;
import pl.plajer.piggybanks.piggy.PiggyManager;
import pl.plajer.piggybanks.utils.ConfigurationManager;
import pl.plajer.piggybanks.utils.LanguageMigrator;
import pl.plajer.piggybanks.utils.MetricsLite;
import pl.plajer.piggybanks.utils.UpdateChecker;
import pl.plajer.piggybanks.utils.Utils;

public class Main extends JavaPlugin {

  private boolean forceDisable = false;
  private List<String> filesToGenerate = Arrays.asList("messages", "piggybanks", "users");
  @Getter
  private PiggyListeners piggyListeners;
  @Getter
  private PiggyManager piggyManager;
  @Getter
  private Economy economy = null;

  @Override
  public void onEnable() {
    for (String plugin : Arrays.asList("Vault", "HolographicDisplays")) {
      if (!getServer().getPluginManager().isPluginEnabled(plugin)) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[PiggyBanks] " + plugin + " dependency not found!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[PiggyBanks] Plugin is turning off...");
        forceDisable = true;
        getServer().getPluginManager().disablePlugin(this);
        return;
      }
    }
    if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[PiggyBanks] Detected ProtocolLib plugin!");
      Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[PiggyBanks] Enabling private statistic holograms.");
    } else {
      Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[PiggyBanks] ProtocolLib plugin isn't installed!");
      Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[PiggyBanks] Disabling private statistic holograms.");
    }
    saveDefaultConfig();
    new ConfigurationManager(this);
    for (String file : filesToGenerate) {
      ConfigurationManager.getConfig(file);
    }
    LanguageMigrator.configUpdate();
    new Commands(this);
    new MenuHandler(this);
    piggyListeners = new PiggyListeners(this);
    piggyManager = new PiggyManager(this);
    new MetricsLite(this);
    piggyManager.loadPiggyBanks();
    piggyManager.teleportScheduler();
    setupEconomy();

    String currentVersion = "v" + Bukkit.getPluginManager().getPlugin("PiggyBanks").getDescription().getVersion();
    if (this.getConfig().getBoolean("update-notify")) {
      try {
        UpdateChecker.checkUpdate(currentVersion);
        String latestVersion = UpdateChecker.getLatestVersion();
        if (latestVersion != null) {
          latestVersion = "v" + latestVersion;
          Bukkit.getConsoleSender().sendMessage(Utils.colorMessage("Other.Plugin-Up-To-Date").replaceAll("%old%", currentVersion).replaceAll("%new%", latestVersion));
        }
      } catch (Exception ex) {
        Bukkit.getConsoleSender().sendMessage(Utils.colorMessage("Other.Plugin-Update-Check-Failed"));
      }
    }
  }

  @Override
  public void onDisable() {
    if (forceDisable) {
      return;
    }
    for (PiggyBank pgb : piggyManager.getLoadedPiggyBanks()) {
      pgb.getPiggyHologram().delete();
    }
    getPiggyManager().getLoadedPiggyBanks().clear();
  }

  private void setupEconomy() {
    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return;
    }
    economy = rsp.getProvider();
  }
}
