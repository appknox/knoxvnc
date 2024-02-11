package com.appknox.vnc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {

    public static String getProp(String prop) {
        String result = "";
        try {
            Process process = new ProcessBuilder().command("/system/bin/getprop", prop).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            result = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}

