package com.timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by danger on 15/9/23.
 */
public class FileUtil {
    public static void saveToFile(File f, String s) {
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(s.getBytes("utf-8"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadFileToString(File f) {
        try {
            int len = (int) f.length();
            byte data[] = new byte[len];
            FileInputStream fis = new FileInputStream(f);
            fis.read(data);

            return new String(data, "utf-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
