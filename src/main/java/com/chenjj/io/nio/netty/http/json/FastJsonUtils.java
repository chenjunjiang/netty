package com.chenjj.io.nio.netty.http.json;

import com.alibaba.fastjson.JSON;

public class FastJsonUtils {
    public static String convertObjectToJSON(Object object) {
        return JSON.toJSONString(object);
    }

    public static Object convertJSONToObject(String json, Class clazz) {
        return JSON.parseObject(json, clazz);
    }
}
