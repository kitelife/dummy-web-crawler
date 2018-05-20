package cn.xiayf.code.dwc.helper;

import java.util.HashMap;
import java.util.Map;

public class ContentTypeHelper {

    private static final Map<String, String> ContentType2FileSuffix = new HashMap<String, String>() {
        {
            put("text/html", ".html");
            put("text/css", ".css");
            put("text/plain", ".txt");
            put("image/jpeg", ".jpeg");
            put("image/png", ".png");
            put("image/x-icon", ".ico");
            put("image/gif", ".gif");
            put("application/x-javascript", ".js");
        }
    };

    public static String suffix(String contentType) {
        return ContentType2FileSuffix.getOrDefault(contentType, "");
    }
}
