package universum.util;

import java.io.*;
import java.util.*;

/**
 * Provides the interface to access configuration storage.
 *
 * @author pan
 */
public class ConfigFile extends Properties {
    private String name;

    public ConfigFile(String name) {
        this.name = name;
        doLoad(Util.findResourceAsStream(name));
    }

    public ConfigFile(File f) throws IOException {
        doLoad(new FileInputStream(f));
    }

    public void doLoad(InputStream is) {
        try {
            load(is);
            is.close();
        } catch (Exception e) {}
    }

    public int getInteger(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.valueOf(getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public float getFloat(String key, float defaultValue) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }


    public String[] getStrings(String key) {
        return getProperty(key).split("\\|");
    }

    public String getStrings(String key, int index) {
        String[] array = getStrings(key);
        return index < array.length ? array[index] : "";
    }

    public void setInteger(String key, int value) {
        setProperty(key, String.valueOf(value));
    }

}
