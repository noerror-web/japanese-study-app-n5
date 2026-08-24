import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateGrammar {
    private static final String SRC_FILE = "app/src/main/assets/n5_grammar_data.json";
    private static final String DST_FILE = "app/src/main/assets/n5_grammar_data_en.json";
    private static final String CACHE_FILE = "translation_cache.txt";
    
    private static final Map<String, String> cache = new HashMap<>();

    private static final Pattern JP_PATTERN = Pattern.compile(
        "[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF\u3000-\u303F\uFF00-\uFFEF]+"
    );

    public static void main(String[] args) {
        try {
            loadCache();
            System.out.println("Loaded " + cache.size() + " translations from cache.");
            
            File src = new File(SRC_FILE);
            if (!src.exists()) {
                System.err.println("Source file not found at: " + src.getAbsolutePath());
                return;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
            
            // Count total lines first
            int totalLines = 0;
            while (reader.readLine() != null) {
                totalLines++;
            }
            reader.close();
            
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(src), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DST_FILE), StandardCharsets.UTF_8));
            
            String line;
            int currentLine = 0;
            boolean inRules = false;
            
            while ((line = reader.readLine()) != null) {
                currentLine++;
                
                if (line.contains("\"rules\":")) {
                    inRules = true;
                }
                
                String processedLine = processLine(line, inRules);
                
                if (inRules && line.contains("]")) {
                    inRules = false;
                }
                
                writer.write(processedLine);
                writer.newLine();
                
                if (currentLine % 100 == 0) {
                    System.out.println("Processed " + currentLine + " / " + totalLines + " lines.");
                    writer.flush();
                }
            }
            
            reader.close();
            writer.close();
            System.out.println("Grammar translation complete! Output written to: " + DST_FILE);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                int idx = line.indexOf("|||");
                if (idx != -1) {
                    String bn = line.substring(0, idx);
                    String en = line.substring(idx + 3);
                    cache.put(bn, en);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void saveToCache(String bn, String en) {
        cache.put(bn, en);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CACHE_FILE, true), StandardCharsets.UTF_8))) {
            bw.write(bn + "|||" + en);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasBengali(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0980' && c <= '\u09FF') {
                return true;
            }
        }
        return false;
    }

    public static boolean hasJapanese(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '\u3040' && c <= '\u309F') || 
                (c >= '\u30A0' && c <= '\u30FF') || 
                (c >= '\u4E00' && c <= '\u9FFF') ||
                (c >= '\u3000' && c <= '\u303F') ||
                (c >= '\uFF00' && c <= '\uFFEF')) {
                return true;
            }
        }
        return false;
    }

    public static int findFirstBengaliChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0980' && c <= '\u09FF') {
                return i;
            }
        }
        return -1;
    }

    public static String processLine(String line, boolean inRules) {
        if (line.contains("\"title\"")) {
            return replaceValueRegex(line, "\"title\"");
        } else if (line.contains("\"text\"")) {
            return replaceValueRegex(line, "\"text\"");
        } else if (inRules) {
            int firstQuote = line.indexOf("\"");
            int lastQuote = line.lastIndexOf("\"");
            if (firstQuote != -1 && lastQuote > firstQuote) {
                String val = line.substring(firstQuote + 1, lastQuote);
                String unescaped = unescapeJson(val);
                if (hasBengali(unescaped)) {
                    String translated = translateString(unescaped);
                    String escaped = escapeJson(translated);
                    return line.substring(0, firstQuote + 1) + escaped + line.substring(lastQuote);
                }
            }
        }
        return line;
    }

    public static String replaceValueRegex(String line, String key) {
        int keyIdx = line.indexOf(key);
        if (keyIdx == -1) return line;
        
        int colonIdx = line.indexOf(":", keyIdx + key.length());
        if (colonIdx == -1) return line;
        
        int valStart = line.indexOf("\"", colonIdx + 1);
        if (valStart == -1) return line;
        
        int valEnd = findClosingQuote(line, valStart + 1);
        if (valEnd != -1) {
            String val = line.substring(valStart + 1, valEnd);
            String unescaped = unescapeJson(val);
            if (hasBengali(unescaped)) {
                String translated = translateString(unescaped);
                String escaped = escapeJson(translated);
                return line.substring(0, valStart + 1) + escaped + line.substring(valEnd);
            }
        }
        return line;
    }

    public static int findClosingQuote(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                return i;
            } else if (c == '\\') {
                i++;
            }
            i++;
        }
        return -1;
    }

    public static String preProcessBengali(String s) {
        if (s == null || s.isEmpty()) return s;

        // Specific Japanese negative verb endings cleanup first
        s = s.replaceAll("(?i)জা\\s*/\\s*দেওয়া\\s*আরিমাসেন", "ja / dewa arimasen");
        s = s.replaceAll("(?i)জা\\s*/\\s*দেওয়া\\s*আরিমাসেন", "ja / dewa arimasen");
        s = s.replaceAll("(?i)দেওয়া\\s*আরিমাসেন\\s*দেশতা", "dewa arimasen deshita");
        s = s.replaceAll("(?i)দেওয়া\\s*আরিমাসেন\\s*দেশতা", "dewa arimasen deshita");
        s = s.replaceAll("(?i)জা\\s*আরিমাসেন\\s*দেশতা", "ja arimasen deshita");
        s = s.replaceAll("(?i)দেওয়া\\s*আরিমাসেন", "dewa arimasen");
        s = s.replaceAll("(?i)দেওয়া\\s*আরিমাসেন", "dewa arimasen");
        
        // Parenthesized Bengali phonetic endings -> clean Romaji equivalent
        s = s.replaceAll("(?i)\\(\\s*ওয়া\\s*\\)", "(wa)");
        s = s.replaceAll("(?i)\\(\\s*ওয়া\\s*\\)", "(wa)");
        s = s.replaceAll("(?i)\\(\\s*হা\\s*\\)", "(ha)");
        s = s.replaceAll("(?i)\\(\\s*দেস\\s*\\)", "(desu)");
        s = s.replaceAll("(?i)\\(\\s*দেসকা\\s*\\)", "(desu ka)");
        s = s.replaceAll("(?i)\\(\\s*দেশতা\\s*\\)", "(deshita)");
        s = s.replaceAll("(?i)\\(\\s*আরিমাসেন\\s*\\)", "(arimasen)");
        s = s.replaceAll("(?i)\\(\\s*মাস\\s*\\)", "(masu)");
        s = s.replaceAll("(?i)\\(\\s*মাসকা\\s*\\)", "(masu ka)");
        s = s.replaceAll("(?i)\\(\\s*কোরে\\s*\\)", "(kore)");
        s = s.replaceAll("(?i)\\(\\s*সোরে\\s*\\)", "(sore)");
        s = s.replaceAll("(?i)\\(\\s*আরে\\s*\\)", "(are)");
        s = s.replaceAll("(?i)\\(\\s*কোকো\\s*\\)", "(koko)");
        s = s.replaceAll("(?i)\\(\\s*সোকো\\s*\\)", "(soko)");
        s = s.replaceAll("(?i)\\(\\s*আসোকো\\s*\\)", "(asoko)");
        s = s.replaceAll("(?i)\\(\\s*কোচিরা\\s*\\)", "(kochira)");
        s = s.replaceAll("(?i)\\(\\s*সোচিরা\\s*\\)", "(sochira)");
        s = s.replaceAll("(?i)\\(\\s*আচিরা\\s*\\)", "(achira)");
        s = s.replaceAll("(?i)\\(\\s*দোকো\\s*\\)", "(doko)");
        s = s.replaceAll("(?i)\\(\\s*দোচিরা\\s*\\)", "(dochira)");
        s = s.replaceAll("(?i)\\(\\s*জি\\s*\\)", "(ji)");
        s = s.replaceAll("(?i)\\(\\s*পুন\\s*\\)", "(pun)");
        s = s.replaceAll("(?i)\\(\\s*ফুন\\s*\\)", "(fun)");
        s = s.replaceAll("(?i)\\(\\s*প্পুন\\s*\\)", "(ppun)");
        s = s.replaceAll("(?i)\\(\\s*নি\\s*\\)", "(ni)");
        s = s.replaceAll("(?i)\\(\\s*দে\\s*\\)", "(de)");
        s = s.replaceAll("(?i)\\(\\s*তো\\s*\\)", "(to)");
        s = s.replaceAll("(?i)\\(\\s*নো\\s*\\)", "(no)");
        s = s.replaceAll("(?i)\\(\\s*মো\\s*\\)", "(mo)");
        s = s.replaceAll("(?i)\\(\\s*কা\\s*\\)", "(ka)");

        // Stand-alone phonetic terms which don't conflict with Bengali words
        s = s.replaceAll("দেসকা", "desu ka");
        s = s.replaceAll("দেস", "desu");
        s = s.replaceAll("দেশতা", "deshita");
        s = s.replaceAll("আরিমাসেন", "arimasen");
        s = s.replaceAll("গোচিসৌসামা", "gochisoosama");
        
        // Japanese character combined with parenthesized Bengali phonetic
        s = s.replaceAll("ます\\s*\\(\\s*মাস\\s*\\)", "ます (masu)");
        s = s.replaceAll("です\\s*\\(\\s*দেস\\s*\\)", "です (desu)");
        s = s.replaceAll("ました\\s*\\(\\s*দেশতা\\s*\\)", "ました (deshita)");
        s = s.replaceAll("ません\\s*\\(\\s*আরিমাসেন\\s*\\)", "ません (arimasen)");
        s = s.replaceAll("ですか\\s*\\(\\s*দেসকা\\s*\\)", "ですか (desu ka)");
        
        return s;
    }

    public static String translateBracketsAndParentheses(String s) {
        // Find [...] containing Bengali
        Pattern bracketPattern = Pattern.compile("\\[([^\\]]*)\\]");
        Matcher bm = bracketPattern.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (bm.find()) {
            String inside = bm.group(1);
            if (hasBengali(inside)) {
                String translated = translateWithProtection(inside);
                bm.appendReplacement(sb, Matcher.quoteReplacement("[" + translated + "]"));
            } else {
                bm.appendReplacement(sb, Matcher.quoteReplacement(bm.group()));
            }
        }
        bm.appendTail(sb);
        s = sb.toString();

        // Find (...) containing Bengali
        Pattern parenPattern = Pattern.compile("\\(([^\\)]*)\\)");
        Matcher pm = parenPattern.matcher(s);
        sb = new StringBuffer();
        while (pm.find()) {
            String inside = pm.group(1);
            if (hasBengali(inside)) {
                String translated = translateWithProtection(inside);
                pm.appendReplacement(sb, Matcher.quoteReplacement("(" + translated + ")"));
            } else {
                pm.appendReplacement(sb, Matcher.quoteReplacement(pm.group()));
            }
        }
        pm.appendTail(sb);
        s = sb.toString();

        return s;
    }

    public static String translateString(String s) {
        if (!hasBengali(s)) {
            return s;
        }

        // Apply pre-processing of Bengali phonetics
        s = preProcessBengali(s);

        // Translate multiple brackets and parentheses containing Bengali
        s = translateBracketsAndParentheses(s);

        if (!hasBengali(s)) {
            return s;
        }

        // Check for example sentence (starts with Japanese prefix and has Bengali translation suffix)
        int firstBnIdx = findFirstBengaliChar(s);
        if (firstBnIdx != -1) {
            String prefix = s.substring(0, firstBnIdx);
            if (hasJapanese(prefix)) {
                String jp = prefix.trim();
                String bn = s.substring(firstBnIdx).trim();
                if (bn.endsWith("।")) {
                    bn = bn.substring(0, bn.length() - 1).trim();
                }
                if (hasBengali(bn)) {
                    String en = translateWithProtection(bn);
                    if (en != null && !en.isEmpty()) {
                        return jp + " [" + en + "]";
                    }
                }
            }
        }

        // Fallback: translate the whole string with protection
        return translateWithProtection(s);
    }

    public static String translateWithProtection(String text) {
        if (!hasBengali(text)) {
            return text;
        }

        List<String> jpBlocks = new ArrayList<>();
        Matcher matcher = JP_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        int placeholderIndex = 0;
        
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            String jpBlock = matcher.group();
            jpBlocks.add(jpBlock);
            sb.append(" _JP").append(placeholderIndex).append("_ ");
            placeholderIndex++;
            lastEnd = matcher.end();
        }
        sb.append(text, lastEnd, text.length());
        
        String stringToTranslate = sb.toString();
        
        // Translate the string using API
        String translated = fetchTranslation(stringToTranslate);
        
        // Restore Japanese blocks
        for (int i = 0; i < jpBlocks.size(); i++) {
            translated = translated.replaceAll("(?i)_\\s*JP\\s*" + i + "\\s*_", Matcher.quoteReplacement(jpBlocks.get(i)));
        }
        
        // Clean up any double spaces introduced by placeholders
        translated = translated.replaceAll("\\s+", " ").trim();
        
        return translated;
    }

    private static String fetchTranslation(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return text;
        
        if (cache.containsKey(trimmed)) {
            return cache.get(trimmed);
        }
        
        int retries = 5;
        long backoff = 1000;
        
        while (retries > 0) {
            try {
                // Sleep for rate-limiting (80ms default)
                Thread.sleep(80);
                
                String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=bn&tl=en&dt=t&q=" 
                        + URLEncoder.encode(trimmed, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 429) {
                    System.out.println("Rate limited (429). Retrying after " + backoff + "ms...");
                    Thread.sleep(backoff);
                    retries--;
                    backoff *= 2;
                    continue;
                }
                
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                String translation = parseTranslation(response.toString());
                if (translation != null && !translation.trim().isEmpty()) {
                    saveToCache(trimmed, translation);
                    return translation;
                }
                
                return text; // fallback
            } catch (Exception e) {
                System.out.println("Error translating: " + trimmed + ". Error: " + e.getMessage() + ". Retrying...");
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                retries--;
                backoff *= 2;
            }
        }
        
        System.out.println("Failed to translate: " + trimmed);
        return text;
    }

    public static String parseTranslation(String json) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        if (json.startsWith("[[[\"")) {
            i = 4;
        } else if (json.startsWith("[[\"")) {
            i = 3;
        } else {
            return json;
        }
        
        while (i < json.length()) {
            StringBuilder sb = new StringBuilder();
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"') {
                    break;
                } else if (c == '\\') {
                    i++;
                    if (i < json.length()) {
                        char next = json.charAt(i);
                        if (next == 'n') sb.append('\n');
                        else if (next == 't') sb.append('\t');
                        else if (next == 'r') sb.append('\r');
                        else sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
                i++;
            }
            
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(sb.toString().trim());
            
            int nextPair = json.indexOf("],[\"", i);
            if (nextPair == -1) {
                break;
            }
            i = nextPair + 4;
        }
        
        return result.toString();
    }

    public static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                if (i < s.length()) {
                    char next = s.charAt(i);
                    if (next == 'n') sb.append('\n');
                    else if (next == 't') sb.append('\t');
                    else if (next == 'r') sb.append('\r');
                    else if (next == '"') sb.append('"');
                    else if (next == '\\') sb.append('\\');
                    else if (next == 'u') {
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                            }
                            i += 4;
                        }
                    } else sb.append(next);
                }
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    public static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\t') sb.append("\\t");
            else if (c == '\r') sb.append("\\r");
            else sb.append(c);
        }
        return sb.toString();
    }
}
