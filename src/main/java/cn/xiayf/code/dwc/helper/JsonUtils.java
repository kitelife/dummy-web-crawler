// Copyright (C) 2017 Baidu Inc. All rights reserved.

package cn.xiayf.code.dwc.helper;

import java.lang.reflect.Type;

import com.google.gson.Gson;

public class JsonUtils {

    private static final Gson GSON = new Gson();

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String str, Type type) {
        return GSON.fromJson(str, type);
    }

    public static <T> T fromJson(String str, Class<T> type) {
        return GSON.fromJson(str, type);
    }
}
