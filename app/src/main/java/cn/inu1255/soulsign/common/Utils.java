package cn.inu1255.soulsign.common;

public class Utils {
    public static String join(Object[] ss, Object key) {
        if (ss == null || ss.length < 1) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(ss[0]);
        for (int i = 1; i < ss.length; i++) {
            sb.append(key);
            sb.append(ss[i]);
        }
        return sb.toString();
    }

    public static String join(int[] ss, Object key) {
        if (ss == null || ss.length < 1) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(ss[0]);
        for (int i = 1; i < ss.length; i++) {
            sb.append(key);
            sb.append(ss[i]);
        }
        return sb.toString();
    }
}
