package VoiceChatMute.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class Utils {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String colorize(String msg) {
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        Matcher match = HEX_PATTERN.matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (match.find()) {
            String color = match.group();
            match.appendReplacement(buffer, ChatColor.of(color).toString());
        }
        match.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
