package cn.xiayf.code.helper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonHelper {

    public static final String Charset_UTF8 = "UTF-8";

    private static MessageDigest md;

    public static String md5Hash(String url) {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("md5");
            } catch (NoSuchAlgorithmException e) {
                log.error(e.getMessage(), e);
            }
        }
        byte[] md5Bytes = md.digest(url.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : md5Bytes) {
            int bt = b & 0xff;
            if (bt < 16) {
                sb.append(0);
            }
            sb.append(Integer.toHexString(bt));
        }
        return sb.toString();
    }

    public static boolean sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
            return true;
        } catch (InterruptedException e) {
            log.error("Thread is interrupted.", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
