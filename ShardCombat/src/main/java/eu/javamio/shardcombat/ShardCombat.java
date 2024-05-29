package eu.javamio.shardcombat;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class ShardCombat extends JavaPlugin implements Listener {
    private int duration;
    private String combatMessage;
    private List<String> blockedCommands;
    private String blockedMessage;
    private String leftCombatMessage;
    private final HashMap<UUID, BukkitRunnable> combatLoggers = new HashMap();
    private final HashMap<UUID, Integer> combatTimers = new HashMap();

    public static Plugin getInstance() {
        return null;
    }


    public void onEnable() {
        this.saveDefaultConfig();

        this.duration = this.getConfig().getInt("duration");
        this.combatMessage = this.translate(this.getConfig().getString("action-bar.combat"));
        this.blockedCommands = this.getConfig().getStringList("blocked-commands");
        this.blockedMessage = this.translate(this.getConfig().getString("blocked-message"));
        this.leftCombatMessage = this.translate(this.getConfig().getString("message.left-combat"));
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
                Player attacker = (Player)event.getDamager();
                Player victim = (Player)event.getEntity();
                this.setInCombat(attacker);
                this.setInCombat(victim);
            }

        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        if (this.isInCombat(player)) {
            ((BukkitRunnable)this.combatLoggers.get(player.getUniqueId())).cancel();
            this.combatLoggers.remove(player.getUniqueId());
            this.combatTimers.remove(player.getUniqueId());
        }

    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (this.isInCombat(player) && this.blockedCommands.contains(event.getMessage().split(" ")[0].substring(1))) {
            event.setCancelled(true);
            player.sendMessage(this.translate(this.blockedMessage.replace("{command}", event.getMessage())));
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.isInCombat(player)) {
            player.setHealth(0.0D);
            ((BukkitRunnable)this.combatLoggers.get(player.getUniqueId())).cancel();
            this.combatLoggers.remove(player.getUniqueId());
            this.combatTimers.remove(player.getUniqueId());
        }

    }

    private void setInCombat(final Player player) {
        if (this.isInCombat(player)) {
            ((BukkitRunnable)this.combatLoggers.get(player.getUniqueId())).cancel();
        }

        this.combatTimers.put(player.getUniqueId(), this.duration);
        BukkitRunnable combatTask = new BukkitRunnable() {
            public void run() {
                int remainingTime = (Integer)ShardCombat.this.combatTimers.get(player.getUniqueId()) - 1;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ShardCombat.this.translate(ShardCombat.this.combatMessage.replace("{timer}", String.valueOf(remainingTime)))));
                if (remainingTime <= 0) {
                    ShardCombat.this.combatLoggers.remove(player.getUniqueId());
                    ShardCombat.this.combatTimers.remove(player.getUniqueId());
                    player.sendMessage(ShardCombat.this.translate(ShardCombat.this.leftCombatMessage));
                    this.cancel();
                } else {
                    ShardCombat.this.combatTimers.put(player.getUniqueId(), remainingTime);
                }

            }
        };
        combatTask.runTaskTimer(this, 0L, 20L);
        this.combatLoggers.put(player.getUniqueId(), combatTask);
    }

    private boolean isInCombat(Player player) {
        return this.combatLoggers.containsKey(player.getUniqueId());
    }

    public String hey(String message) {

        String hey = ChatColor.translateAlternateColorCodes('&', message);

        return hey;
    }

    private String translate(String message) {
        String translated = ChatColor.translateAlternateColorCodes('&', message);
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");

        String colorCode;
        ChatColor hexColor;
        for(Matcher matcher = hexPattern.matcher(translated); matcher.find(); translated = translated.replace(colorCode, hexColor.toString())) {
            colorCode = matcher.group();
            hexColor = ChatColor.of(colorCode.substring(1));
        }

        return translated;
    }
}

