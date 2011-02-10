/* Copyright (c) 2010 Richard Chan */
package helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gdata.util.common.base.StringUtil;

public class Helpers {

    public abstract static class CallbackWithException<C, E extends Exception> {
        public abstract void call(C obj) throws E;
    }
    public abstract static class CallbackWithReturn<C, R> {
        public abstract R call(C obj);
    }
    
    public static <E extends Exception> void filterInPlace(String contains, Collection<String> set, boolean findOne, CallbackWithException<String,E> callback) 
    throws E {
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            String s = it.next();
            if (s.toUpperCase().indexOf(contains) > -1) {
                callback.call(s);
                it.remove();
                if (findOne) return;
            }
        }
    }
    
    public static String readFile(Class<?> cls, String filename) throws IOException {
        InputStream stream = cls.getResourceAsStream(filename);
        if (stream != null) {
            StringBuffer fileData = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            char[] buf = new char[1024];
            for (int bufLen = reader.read(buf); bufLen != -1; bufLen = reader.read(buf)){
                String readData = String.valueOf(buf, 0, bufLen);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
            return fileData.toString();
        }
        throw new IOException("file cannot be found: " + filename);
    }
    
    public static String writeJSAssign(String name, String value) {
        StringBuffer sb = new StringBuffer();
        return sb
          .append(name).append(" = ").append(value).append(";")
          .toString();

    }
    
    public static String writeJSVar(String name, String value) {
        return writeJSAssign("var " + name, value);
    }
    
    @SuppressWarnings("deprecation")
    public static String writeJSArray(String[] ary) {
        StringBuffer sb = new StringBuffer();
        return sb
          .append("[").append(StringUtil.join(ary, ",")).append("]")
          .toString();
    }
    
    public static String writeJSMap(String[] keys, String[] values) {
        StringBuffer sb = new StringBuffer().append("{");
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) sb.append(",");
            sb.append(keys[i]).append(": ").append(values[i]);
        }
        return sb.append("}\n").toString();
    }
    
    public static String writeJSMap(Map<String, String> map) {
        String[] keys = new String[map.size()];
        String[] values = new String[map.size()];
        int i = 0;
        for (Entry<String,String> e : map.entrySet()) {
            keys[i] = e.getKey();
            values[i++] = e.getValue();
        }
        return writeJSMap(keys, values);
    }
    
    public static final DecimalFormat gradeDecimalformat = new DecimalFormat("#,###0.00");
    
    public static Map<String, String> vfp(String v, String f, String p) {
        Map<String, String> map = new HashMap<String, String>();
        if (v != null) map.put("v", v);
        if (f != null) map.put("f", f);
        if (p != null) map.put("p", p);
        return map;
    }
    
    public static String addSingleQuote(String s) {
        return "'" + s + "'";
    }
    
    public static Map<String, String> ary2Map(String[] ary) {
        if (ary.length % 2 != 0)
            throw new RuntimeException("Bad format for ary2Map.");
        Map<String,String> map = new HashMap<String, String>();
        for (int i = 0; i < ary.length; i += 2) {
            map.put(ary[i], ary[i+1]);
        }
        return map;
    }
    
}
