package com.nowcoder.community;

import java.io.IOException;

public class WkTests {

    public static void main(String[] args) {
        String cmd = "D:/java/wkhtmltox/bin/wkhtmltoimage.exe --quality 75 https://www.nowcoder.com D:/java/wkhtmltox/bin/img/1.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
