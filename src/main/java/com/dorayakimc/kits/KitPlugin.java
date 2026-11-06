package com.dorayakimc.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class KitPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private FileConfiguration config;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, String> editingKits = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        if (getCommand("kit") != null) {
            getCommand("kit").setExecutor(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("DorayakiMC Kits v1.1 avviato con successo!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Comando eseguibile solo in gioco.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("dorayakimc.admin")) {
                    player.sendMessage(colorize("&cNon hai il permesso per ricaricare il plugin."));
                    return true;
                }
                reloadConfig();
                config = getConfig();
                player.sendMessage(colorize("&2&l[ᴅᴏʀᴀʏᴀᴋɪᴍᴄ] &aConfigurazione ricaricata!"));
                return true;
            }

            if (args[0].equalsIgnoreCase("editor")) {
                if (!player.hasPermission("dorayakimc.admin")) {
                    player.sendMessage(colorize("&cNon hai il permesso per accedere all'editor."));
                    return true;
                }
                openEditorGui(player);
                return true;
            }
        }

        // Controllo mondi abilitati
        List<String> allowedWorlds = config.getStringList("allowed-worlds");
        if (allowedWorlds != null && !allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
            String msg = config.getString("messages.world-blocked", "&cNon puoi usare i kit qui!");
            player.sendMessage(colorize(msg));
            return true;
        }

        openKitGui(player);
        return true;
    }

    private void openKitGui(Player player) {
        String title = colorize("&2&lᴅᴏʀᴀʏᴀᴋɪᴍᴄ &8- &a&lᴍᴇɴù ᴋɪᴛ");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ConfigurationSection kitsSec = config.getConfigurationSection("kits");
        if (kitsSec != null) {
            for (String kitKey : kitsSec.getKeys(false)) {
                int slot = kitsSec.getInt(kitKey + ".slot", 10);
                String name = kitsSec.getString(kitKey + ".display-name", "&aKit " + kitKey);
                String matName = kitsSec.getString(kitKey + ".icon", "EMERALD");
                int cd = kitsSec.getInt(kitKey + ".cooldown", 0);
                String perm = kitsSec.getString(kitKey + ".permission", "");

                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.EMERALD;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(colorize(name));
                    List<String> lore = new ArrayList<>();
                    lore.add(colorize("&7Clicca per riscattare questo kit."));
                    if (cd > 0) lore.add(colorize("&8Cooldown: &e" + (cd / 60) + " min"));
                    if (!perm.isEmpty()) lore.add(colorize("&8Permesso: &7" + perm));
                    lore.add(colorize("&8ID: " + kitKey));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                gui.setItem(slot, item);
            }
        }

        player.openInventory(gui);
    }

    private void openEditorGui(Player player) {
        String title = colorize("&4&l[EDITOR] &8- &c&lɢᴇsᴛɪᴏɴᴇ ᴋɪᴛ");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ConfigurationSection kitsSec = config.getConfigurationSection("kits");
        if (kitsSec != null) {
            for (String kitKey : kitsSec.getKeys(false)) {
                int slot = kitsSec.getInt(kitKey + ".slot", 10);
                String name = kitsSec.getString(kitKey + ".display-name", "&aKit " + kitKey);
                Material mat = Material.CHEST;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(colorize(name));
                    meta.setLore(Arrays.asList(
                            colorize("&eTasto Destro: &7Modifica Oggetti Kit"),
                            colorize("&8ID: " + kitKey)
                    ));
                    item.setItemMeta(meta);
                }
                gui.setItem(slot, item);
            }
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title == null) return;

        Player player = (Player) event.getWhoClicked();

        // 1. BLOCCO FURTO ITEM GUI PLAYER
        if (title.contains("ᴅᴏʀᴀʏᴀᴋɪᴍᴄ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                for (String line : clicked.getItemMeta().getLore()) {
                    String plainLine = ChatColor.stripColor(line);
                    if (plainLine != null && plainLine.contains("ID: ")) {
                        String kitId = plainLine.replace("ID: ", "").trim();
                        giveKit(player, kitId);
                        player.closeInventory();
                        break;
                    }
                }
            }
        }

        // 2. BLOCCO ED EDITOR ADMIN
        if (title.contains("[EDITOR]")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                for (String line : clicked.getItemMeta().getLore()) {
                    String plainLine = ChatColor.stripColor(line);
                    if (plainLine != null && plainLine.contains("ID: ")) {
                        String kitId = plainLine.replace("ID: ", "").trim();
                        if (event.isRightClick()) {
                            openContentEditor(player, kitId);
                        }
                        break;
                    }
                }
            }
        }
    }

    private void openContentEditor(Player player, String kitId) {
        editingKits.put(player.getUniqueId(), kitId);
        String title = colorize("&8Editor Oggetti: &e" + kitId);
        Inventory gui = Bukkit.createInventory(null, 36, title);

        List<?> items = config.getList("kits." + kitId + ".items");
        if (items != null) {
            for (int i = 0; i < items.size() && i < 36; i++) {
                if (items.get(i) instanceof ItemStack) {
                    gui.setItem(i, (ItemStack) items.get(i));
                }
            }
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (editingKits.containsKey(player.getUniqueId())) {
            String kitId = editingKits.remove(player.getUniqueId());
            Inventory inv = event.getInventory();
            List<ItemStack> itemsToSave = new ArrayList<>();

            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    itemsToSave.add(item);
                }
            }

            config.set("kits." + kitId + ".items", itemsToSave);
            saveConfig();
            player.sendMessage(colorize("&2&l[ᴅᴏʀᴀʏᴀᴋɪᴍᴄ] &aOggetti per il kit &e" + kitId + " &asalvati con successo!"));
        }
    }

    private void giveKit(Player player, String kitId) {
        String perm = config.getString("kits." + kitId + ".permission", "");
        if (!perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage(colorize(config.getString("messages.no-permission", "&cNo permesso!")));
            return;
        }

        int cooldownTime = config.getInt("kits." + kitId + ".cooldown", 0);
        if (cooldownTime > 0 && !player.hasPermission("dorayakimc.bypass.cooldown")) {
            long lastUse = cooldowns.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(kitId, 0L);
            long timeLeft = (lastUse + (cooldownTime * 1000L)) - System.currentTimeMillis();

            if (timeLeft > 0) {
                long secondsLeft = timeLeft / 1000;
                String msg = config.getString("messages.cooldown-active", "&cAspetta %time%!")
                        .replace("%time%", secondsLeft + "s");
                player.sendMessage(colorize(msg));
                return;
            }
        }

        List<?> items = config.getList("kits." + kitId + ".items");
        if (items == null || items.isEmpty()) {
            player.sendMessage(colorize("&cQuesto kit è vuoto! Chiedi a un admin di impostarlo via /kit editor."));
            return;
        }

        for (Object obj : items) {
            if (obj instanceof ItemStack) {
                player.getInventory().addItem((ItemStack) obj);
            }
        }

        // Salva Cooldown
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(kitId, System.currentTimeMillis());

        String claimedMsg = config.getString("messages.kit-claimed", "&aKit riscattato!")
                .replace("%kit%", kitId);
        player.sendMessage(colorize(claimedMsg));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
