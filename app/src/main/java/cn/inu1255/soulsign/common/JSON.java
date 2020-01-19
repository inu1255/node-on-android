package cn.inu1255.soulsign.common;

import com.google.gson.Gson;

import java.util.Map;

public class JSON {
    public static Gson gson = new Gson();

    public static String stringify(Object data) {
        return gson.toJson(data);
    }

    public static Map parse(String s) {
        if (s == null) return null;
        return gson.fromJson(s, Map.class);
    }
}
