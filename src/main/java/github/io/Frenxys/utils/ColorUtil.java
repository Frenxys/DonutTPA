package github.io.Frenxys.utils;

public class ColorUtil {

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text);
        int i = 0;
        while (i < sb.length() - 7) {
            if (sb.charAt(i) == '&' && sb.charAt(i + 1) == '#') {
                String hex = sb.substring(i + 2, i + 8);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    StringBuilder repl = new StringBuilder("\u00a7x");
                    for (char c : hex.toCharArray()) {
                        repl.append('\u00a7').append(c);
                    }
                    sb.replace(i, i + 8, repl.toString());
                    i += repl.length();
                    continue;
                }
            }
            ++i;
        }
        return sb.toString().replace('&', '\u00a7');
    }

    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?i)\u00a7[0-9A-FK-ORX]|&#[0-9A-Fa-f]{6}", "");
    }
}
