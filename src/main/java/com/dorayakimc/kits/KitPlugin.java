package com.dorayakimc.kits;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public final class KitPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        
        getCommand("kit").setExecutor(this);
        getCommand("kitsreload").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("DorayakiMC Kits avviato con successo!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kit")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            List<String> allowedWorlds = config.getStringList("allowed-worlds");
            if (allowedWorlds != null && !allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                String msg = config.getString("messages.world-blocked", "&c&l[ᴅᴏʀᴀʏᴀᴋɪᴍᴄ] &cNon puoi usare i kit in questo mondo!");
                player.sendMessage(colorize(msg));
                return true;
            }

            openKitGui(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("kitsreload")) {
            if (!sender.hasPermission("dorayakimc.admin")) return true;
            reloadConfig();
            config = getConfig();
            sender.sendMessage(colorize("&2&l[ᴅᴏʀᴀʏᴀᴋɪᴍᴄ] &aConfigurazione ricaricata con successo!"));
            return true;
        }
        return false;
    }

    private void openKitGui(Player player) {
        String title = createGradientText("ᴅᴏʀᴀʏᴀᴋɪᴍᴄ - ᴍᴇɴù ᴋɪᴛ", new Color(0, 100, 0), new Color(0, 255, 127));
        Inventory gui = Bukkit.createInventory(null, 27, title);

        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < 7; i++) {
            int kitId = i + 1;
            ItemStack item = new ItemStack(Material.EMERALD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(createGradientText("ᴋɪᴛ ɴ. " + kitId, new Color(46, 139, 87), new Color(127, 255, 0)));
                meta.setLore(Arrays.asList(
                        colorize("&7Clicca per riscattare questo kit."),
                        colorize("&8ID: kit" + kitId)
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slots[i], item);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String plainTitle = ChatColor.stripColor(event.getView().getTitle());
        if (plainTitle != null && plainTitle.contains("DORAYAKIMC")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                List<String> lore = clicked.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        String plainLine = ChatColor.stripColor(line);
                        if (plainLine != null && plainLine.contains("ID: kit")) {
                            String kitId = plainLine.replace("ID: kit", "").trim();
                            player.sendMessage(colorize("&2&l[ᴅᴏʀᴀʏᴀᴋɪᴍᴄ] &aHai riscattato il kit &e" + kitId + "&a!"));
                            player.closeInventory();
                        }
                    }
                }
            }
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String createGradientText(String message, Color startColor, Color endColor) {
        StringBuilder builder = new StringBuilder();
        int length = message.length();
        for (int i = 0; i < length; i++) {
            double ratio = (double) i / (double) Math.max(length - 1, 1);
            int red = (int) (startColor.getRed() + ratio * (endColor.getRed() - startColor.getRed()));
            int green = (int) (startColor.getGreen() + ratio * (endColor.getGreen() - startColor.getGreen()));
            int blue = (int) (startColor.getBlue() + ratio * (endColor.getBlue() - startColor.getBlue()));
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of(new Color(red, green, blue));
            builder.append(color).append(message.charAt(i));
        }
        return builder.toString();
    }
}
