package VoiceChatMute;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceChatMute extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerCommands();
        startActionBarTask();
        getLogger().info("VoiceChatMute has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VoiceChatMute has been disabled.");
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("voicechatmute"), "Command 'voicechatmute' is missing from plugin.yml")
                .setExecutor(this);
        Objects.requireNonNull(getCommand("unmutevoicechat"), "Command 'unmutevoicechat' is missing from plugin.yml")
                .setExecutor(this);
        Objects.requireNonNull(getCommand("voicechatinfo"), "Command 'voicechatinfo' is missing from plugin.yml")
                .setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("voicechatmute")) {
            return handleMuteCommand(sender, args);
        }

        if (command.getName().equalsIgnoreCase("unmutevoicechat")) {
            return handleUnmuteCommand(sender, args);
        }

        if (command.getName().equalsIgnoreCase("voicechatinfo")) {
            return handleInfoCommand(sender);
        }

        return false;
    }

    private boolean handleMuteCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.vcmute")) {
            sender.sendMessage(ChatColor.RED + "אין לך הרשאה לבצע פקודה זו!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eשימוש נכון: &b/voicechatmute [שם השחקן] [זמן: 30s/15m/5h/2d] [סיבה אופציונלית]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "השחקן לא מחובר!");
            return true;
        }

        long durationMillis = parseTime(args[1]);
        if (durationMillis <= 0) {
            sender.sendMessage(ChatColor.RED + "פורמט זמן לא תקין! השתמש ב-s, m, h או d.");
            return true;
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        String trimmedReason = reason.toString().trim();
        if (trimmedReason.isEmpty()) {
            trimmedReason = "לא צוינה סיבה";
        }

        String uuid = target.getUniqueId().toString();

        getConfig().set("mutes." + uuid + ".expiry", System.currentTimeMillis() + durationMillis);
        getConfig().set("mutes." + uuid + ".reason", trimmedReason);
        saveConfig();

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " permission settemp voicechat.speak false " + args[1]);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 0.8F);
        target.playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.5F);

        Bukkit.broadcastMessage(ChatColor.WHITE + " ");
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&bהשחקן &f" + target.getName() + " &bקיבל מיוט בוויס צ'אט למשך &e" + args[1] + " &bסיבה: &f" + trimmedReason));
        Bukkit.broadcastMessage(ChatColor.WHITE + " ");
        return true;
    }

    private boolean handleUnmuteCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.vcmute")) {
            sender.sendMessage(ChatColor.RED + "אין לך הרשאה לבצע פקודה זו!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eשימוש נכון: &b/unmutevoicechat [שם השחקן]"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "השחקן לא מחובר!");
            return true;
        }

        String uuid = target.getUniqueId().toString();
        if (!getConfig().contains("mutes." + uuid)) {
            sender.sendMessage(ChatColor.RED + "השחקן הזה לא נמצא במיוט בוויס צ'אט!");
            return true;
        }

        getConfig().set("mutes." + uuid, null);
        saveConfig();

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + target.getName() + " permission unset voicechat.speak");
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.4F);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&aהשחקן &f" + target.getName() + " &aשוחרר מההשתקה בוויס צ'אט על ידי &f" + sender.getName()));
        return true;
    }

    private boolean handleInfoCommand(CommandSender sender) {
        if (!sender.hasPermission("staff.vcinfo")) {
            sender.sendMessage(ChatColor.RED + "אין לך הרשאה לבצע פקודה זו!");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "מצב וויס צ'אט בשרת:");
        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            if (getConfig().contains("mutes." + uuid)) {
                String reason = getConfig().getString("mutes." + uuid + ".reason", "לא צוין");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[✘] &f" + player.getName() + " &7(מושתק - סיבה: " + reason + ")"));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[✔] &f" + player.getName() + " &7(יכול לדבר)"));
            }
        }
        return true;
    }

    private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String uuid = player.getUniqueId().toString();
                    if (!getConfig().contains("mutes." + uuid)) {
                        continue;
                    }

                    long expiry = getConfig().getLong("mutes." + uuid + ".expiry");
                    String reason = getConfig().getString("mutes." + uuid + ".reason", "לא צוין");

                    if (now >= expiry) {
                        getConfig().set("mutes." + uuid, null);
                        saveConfig();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset voicechat.speak");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN + "המיוט שלך בוויס צ'אט הסתיים! חזרת לדבר."));
                        continue;
                    }

                    long timeRemainingMillis = expiry - now;
                    String timeLeftFormatted = formatTimeRemaining(timeRemainingMillis);
                    String message = ChatColor.translateAlternateColorCodes('&', "&bוויס צ'אט נעול סיבה: &f" + reason + " &b| נותר: &e" + timeLeftFormatted);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private long parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return -1L;
        }

        String normalized = timeStr.trim().toLowerCase(Locale.ROOT);
        Pattern pattern = Pattern.compile("(\\d+)([smhd])");
        Matcher matcher = pattern.matcher(normalized);

        long totalMillis = 0L;
        int lastMatchEnd = 0;

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            if (matcher.start() != lastMatchEnd) {
                return -1L;
            }

            switch (unit) {
                case "s":
                    totalMillis += value * 1000L;
                    break;
                case "m":
                    totalMillis += value * 60L * 1000L;
                    break;
                case "h":
                    totalMillis += value * 60L * 60L * 1000L;
                    break;
                case "d":
                    totalMillis += value * 24L * 60L * 60L * 1000L;
                    break;
                default:
                    return -1L;
            }

            lastMatchEnd = matcher.end();
        }

        if (lastMatchEnd != normalized.length()) {
            return -1L;
        }

        return totalMillis;
    }

    private String formatTimeRemaining(long millis) {
        long seconds = millis / 1000L;
        if (seconds < 60L) {
            return seconds + " שניות";
        }

        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + " דקות";
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " שעות";
        }

        long days = hours / 24L;
        return days + " ימים";
    }
}