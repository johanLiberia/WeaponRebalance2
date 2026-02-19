 package com.rebalance;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
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

public final class Main extends JavaPlugin implements Listener {

    private static final UUID DAMAGE_UUID = UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final UUID SPEED_UUID = UUID.fromString("fa233e1c-4180-4865-b01b-a29f9fd81f78");
    private static final UUID ARMOR_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6b");
    private static final UUID TOUGHNESS_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6c");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("845db27c-c624-495f-8c9f-6020a9a58b6d");

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("WeaponRebalance enabled");
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("WeaponRebalance disabled");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null) {
            applyItemStats(item);
        }
        updateArmor(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item != null) {
            applyItemStats(item);
        }
        getServer().getScheduler().runTaskLater(this, () -> updateArmor(player), 1L);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        if (!config.getBoolean("shield.creeper-breaks", true)) return;

        int hitsToBreak = config.getInt("shield.creeper-hits-to-break", 2);
        if (hitsToBreak < 1) hitsToBreak = 2;

        for (org.bukkit.entity.Entity entity : event.getLocation().getWorld().getNearbyEntities(
                event.getLocation(), event.getYield(), event.getYield(), event.getYield())) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand.getType() == org.bukkit.Material.SHIELD) {
                    damageShield(offHand, hitsToBreak);
                }
            }
        }
    }

    private void applyItemStats(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String materialName = item.getType().name();
        boolean modified = false;

        // Swords        if (materialName.endsWith("_SWORD")) {
            modified |= applyWeaponStats(meta, item.getType(), "swords");
        }
        // Axes
        else if (materialName.endsWith("_AXE")) {
            modified |= applyWeaponStats(meta, item.getType(), "axes");
}
        // Special weapons
        else if (materialName.equals("MACE") || materialName.equals("TRIDENT")) {
            modified |= applySpecialWeaponStats(meta, item.getType());
        }
        // Armor
        else if (materialName.endsWith("_HELMET")  materialName.endsWith("_CHESTPLATE") 
                 materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS")) {
            modified |= applyArmorStats(meta, item.getType());
        }

        if (modified) {
            item.setItemMeta(meta);
        }
    }

    private boolean applyWeaponStats(ItemMeta meta, org.bukkit.Material type, String section) {
        String key = type.name();
        if (!config.isConfigurationSection(section + "." + key)) return false;

        double damage = config.getDouble(section + "." + key + ".damage");
        double speed = config.getDouble(section + "." + key + ".speed");

        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);

        double baseDamage = getBaseDamage(type);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID, "generic.attackDamage", damage - baseDamage,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        double vanillaSpeed = 1.6;
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(
                SPEED_UUID, "generic.attackSpeed", speed - vanillaSpeed,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        return true;
    }

    private boolean applySpecialWeaponStats(ItemMeta meta, org.bukkit.Material type) {
        String key = type.name();
        String section = "special";
        if (!config.isConfigurationSection(section + "." + key)) return false;
        double damage = config.getDouble(section + "." + key + ".damage");

        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        double baseDamage = getBaseDamage(type);
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(
                DAMAGE_UUID, "generic.attackDamage", damage - baseDamage,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));

        return true;
    }

    private boolean applyArmorStats(ItemMeta meta, org.bukkit.Material type) {
        String key = type.name();
        if (!config.isConfigurationSection("armor." + key)) return false;

        EquipmentSlot slot = getSlot(type);
        double armor = config.getDouble("armor." + key + ".armor", 0);
        double toughness = config.getDouble("armor." + key + ".toughness", 0);
        double knockback = config.getDouble("armor." + key + ".knockback", 0);

        if (armor > 0) {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(
                    ARMOR_UUID, "generic.armor", armor,
                    AttributeModifier.Operation.ADD_NUMBER, slot));
        }
        if (toughness > 0) {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(
                    TOUGHNESS_UUID, "generic.armorToughness", toughness,
                    AttributeModifier.Operation.ADD_NUMBER, slot));
        }
        if (knockback > 0) {
            meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(
                    KNOCKBACK_UUID, "generic.knockbackResistance", knockback,
                    AttributeModifier.Operation.ADD_NUMBER, slot));
        }

        return armor > 0  toughness > 0  knockback > 0;
    }
private void damageShield(ItemStack shield, int hitsToBreak) {
        if (!(shield.getItemMeta() instanceof Damageable)) return;
        Damageable damageable = (Damageable) shield.getItemMeta();

        int maxDurability = shield.getType().getMaxDurability();
        int damagePerHit = maxDurability / hitsToBreak;
        int currentDamage = damageable.getDamage();
        int newDamage = Math.min(currentDamage + damagePerHit, maxDurability);
        damageable.setDamage(newDamage);
        shield.setItemMeta(damageable);

        if (newDamage >= maxDurability) {
            shield.setAmount(0);
        }
    }

    private void updateArmor(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && !armor.getType().isAir()) {
                applyItemStats(armor);
            }
        }
    }

    private EquipmentSlot getSlot(org.bukkit.Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return EquipmentSlot.CHEST;
    }

    private double getBaseDamage(org.bukkit.Material material) {
        switch (material) {
            case WOODEN_SWORD: return 4.0;
            case STONE_SWORD: return 5.0;
            case IRON_SWORD: return 6.0;
            case GOLDEN_SWORD: return 4.0;
            case DIAMOND_SWORD: return 7.0;
            case NETHERITE_SWORD: return 8.0;
            case WOODEN_AXE: return 7.0;
            case STONE_AXE: return 9.0;
            case IRON_AXE: return 9.0;
            case GOLDEN_AXE: return 7.0;
            case DIAMOND_AXE: return 9.0;
            case NETHERITE_AXE: return 10.0;
            case MACE: return 6.0;
            case TRIDENT: return 9.0;
            default: return 0.0;
        }
    }
}
