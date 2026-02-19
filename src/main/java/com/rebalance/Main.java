package com.rebalance;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener {

    private static final UUID DAMAGE_UUID = UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final UUID SPEED_UUID = UUID.fromString("fa233e1c-4180-4865-b01b-a29f9fd81f78");
    private static final UUID ARMOR_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6b");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6c");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6d");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WeaponRebalance enabled");
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("WeaponRebalance disabled");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null) {
            applyStats(item);
        }
        updateArmor(player);
    }

    @EventHandler    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            applyStats(item);
        }
        getServer().getScheduler().runTaskLater(this, () -> updateArmor(player), 1L);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        if (!getConfig().getBoolean("shield.creeper-breaks", true)) return;
        
        int hits = getConfig().getInt("shield.creeper-hits-to-break", 2);
        if (hits < 1) hits = 2;
        
        for (org.bukkit.entity.Entity e : event.getLocation().getWorld().getNearbyEntities(
                event.getLocation(), event.getYield(), event.getYield(), event.getYield())) {
            if (e instanceof Player) {
                Player p = (Player) e;
                ItemStack off = p.getInventory().getItemInOffHand();
                if (off.getType() == org.bukkit.Material.SHIELD) {
                    damageShield(off, hits);
                }
            }
        }
    }

    private void applyStats(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        String name = item.getType().name();
        
        if (name.endsWith("_SWORD") || name.endsWith("_AXE")) {
            String section = name.endsWith("_SWORD") ? "swords" : "axes";
            if (getConfig().isConfigurationSection(section + "." + name)) {
                double dmg = getConfig().getDouble(section + "." + name + ".damage");
                double spd = getConfig().getDouble(section + "." + name + ".speed");
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                        DAMAGE_UUID, "dmg", dmg - getBaseDmg(item.getType()),
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                        SPEED_UUID, "spd", spd - 1.6,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));                item.setItemMeta(meta);
            }
        }
        
        if (name.equals("MACE") || name.equals("TRIDENT")) {
            if (getConfig().isConfigurationSection("special." + name)) {
                double dmg = getConfig().getDouble("special." + name + ".damage");
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new Attribute
