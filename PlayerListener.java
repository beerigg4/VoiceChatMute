package VoiceChatMute.listeners;

import VoiceChatMute.VoiceChatMute;
import VoiceChatMute.utils.Utils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final VoiceChatMute plugin;

    public PlayerListener(VoiceChatMute plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        if (!plugin.getConfig().contains("mutes." + uuid)) {
            return;
        }

        long now = System.currentTimeMillis();
        long expiry = plugin.getConfig().getLong("mutes." + uuid + ".expiry");

        if (now >= expiry) {
            plugin.getConfig().set("mutes." + uuid, null);
            plugin.saveConfig();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission unset voicechat.speak");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Utils.colorize("&aהמיוט שלך בוויס צ'אט הסתיים! חזרת לדבר.")));
        } else {
            long remainingSeconds = (expiry - now) / 1000L;
            if (remainingSeconds < 1) {
                remainingSeconds = 1;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission settemp voicechat.speak false " + remainingSeconds + "s");
        }
    }
}
