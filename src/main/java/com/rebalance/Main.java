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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        final Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            applyStats(item);
        }
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                updateArmor(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }
        if (!getConfig().getBoolean("shield.creeper-breaks", true)) {
            return;
        }
        int hits = getConfig().getInt("shield.creeper-hits-to-break", 2);
        if (hits < 1) {
            hits = 2;
        }
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
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String name = item.getType().name();        
        if (name.endsWith("_SWORD") || name.endsWith("_AXE")) {
            String section = name.endsWith("_SWORD") ? "swords" : "axes";
            if (getConfig().isConfigurationSection(section + "." + name)) {
                double dmg = getConfig().getDouble(section + "." + name + ".damage");
                double spd = getConfig().getDouble(section + "." + name + ".speed");
meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
                
                double baseDmg = getBaseDmg(item.getType());
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                        DAMAGE_UUID, "dmg", dmg - baseDmg,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                        SPEED_UUID, "spd", spd - 1.6,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                item.setItemMeta(meta);
            }
        }
        
        if (name.equals("MACE") || name.equals("TRIDENT")) {
            if (getConfig().isConfigurationSection("special." + name)) {
                double dmg = getConfig().getDouble("special." + name + ".damage");
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
                double baseDmg = getBaseDmg(item.getType());
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                        DAMAGE_UUID, "dmg", dmg - baseDmg,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
                item.setItemMeta(meta);
            }
        }
        
        if (name.endsWith("_HELMET")  name.endsWith("_CHESTPLATE") 
            name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            if (getConfig().isConfigurationSection("armor." + name)) {
                EquipmentSlot slot = getSlot(item.getType());
                double armor = getConfig().getDouble("armor." + name + ".armor", 0);
                double tough = getConfig().getDouble("armor." + name + ".toughness", 0);
                double kb = getConfig().getDouble("armor." + name + ".knockback", 0);
                
                if (armor > 0) {
                    meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
                    meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                            ARMOR_UUID, "armor", armor,
                            AttributeModifier.Operation.ADD_NUMBER, slot));
                }
                if (tough > 0) {
                    meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
                    meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                            TOUGHNESS_UUID, "tough", tough,                            AttributeModifier.Operation.ADD_NUMBER, slot));
                }
                if (kb > 0) {
                    meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
                    meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                            KNOCKBACK_UUID, "kb", kb,
                            AttributeModifier.Operation.ADD_NUMBER, slot));
                }
                item.setItemMeta(meta);
            }
        }
    }

    private void damageShield(ItemStack shield, int hits) {
        if (!(shield.getItemMeta() instanceof Damageable)) {
            return;
        }
        Damageable d = (Damageable) shield.getItemMeta();
        int max = shield.getType().getMaxDurability();
        int perHit = max / hits;
        int newDmg = d.getDamage() + perHit;
        if (newDmg > max) {
            newDmg = max;
        }
        d.setDamage(newDmg);
        shield.setItemMeta(d);
        if (newDmg >= max) {
            shield.setAmount(0);
        }
    }

    private void updateArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir()) {
                applyStats(armor[i]);
            }
        }
    }
private EquipmentSlot getSlot(org.bukkit.Material m) {
        String n = m.name();
        if (n.endsWith("_HELMET")) {
            return EquipmentSlot.HEAD;
        }
        if (n.endsWith("_CHESTPLATE")) {
            return EquipmentSlot.CHEST;
        }
        if (n.endsWith("_LEGGINGS")) {
            return EquipmentSlot.LEGS;        }
        if (n.endsWith("_BOOTS")) {
            return EquipmentSlot.FEET;
        }
        return EquipmentSlot.CHEST;
    }

    private double getBaseDmg(org.bukkit.Material m) {
        if (m == org.bukkit.Material.WOODEN_SWORD) return 4.0;
        if (m == org.bukkit.Material.STONE_SWORD) return 5.0;
        if (m == org.bukkit.Material.IRON_SWORD) return 6.0;
        if (m == org.bukkit.Material.GOLDEN_SWORD) return 4.0;
        if (m == org.bukkit.Material.DIAMOND_SWORD) return 7.0;
        if (m == org.bukkit.Material.NETHERITE_SWORD) return 8.0;
        if (m == org.bukkit.Material.WOODEN_AXE) return 7.0;
        if (m == org.bukkit.Material.STONE_AXE) return 9.0;
        if (m == org.bukkit.Material.IRON_AXE) return 9.0;
        if (m == org.bukkit.Material.GOLDEN_AXE) return 7.0;
        if (m == org.bukkit.Material.DIAMOND_AXE) return 9.0;
        if (m == org.bukkit.Material.NETHERITE_AXE) return 10.0;
        if (m == org.bukkit.Material.MACE) return 6.0;
        if (m == org.bukkit.Material.TRIDENT) return 9.0;
        return 0.0;
    }
}
