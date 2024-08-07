package com.zmk.ink.reabble.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.FileOutputStream;
import java.io.InputStream;

public class NetUtil {
    public static Bitmap gifToJpg(String gifUrl, String jpgFilePath) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // 创建 URL 连接
            URL url = new URL(gifUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 获取输入流
            inputStream = connection.getInputStream();
            // 解码 GIF 的第一帧为 Bitmap
            bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // 保存 Bitmap 为 JPG 文件
                File file = new File(jpgFilePath);
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }

    public static boolean isGifUrl(String urlString) {
        if (urlString.startsWith("https://reabble.cn")) {
            return false;
        }
        if (urlString == null || urlString.isEmpty()) {
            return false;
        }
        if (urlString.endsWith(".gif") || urlString.endsWith("wx_fmt=gif")) {
            return true;
        }

        // 若网址为一个单纯的文件地址，且文件扩展名不为 .gif ，则返回 false
        try {
            URL url = new URL(urlString);
            String filePath = url.getPath();

            int lastIndexOfDot = filePath.lastIndexOf('.');
            if (lastIndexOfDot != -1) {
                return false;
            }
        } catch (MalformedURLException e) {
            return false;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            String contentType = connection.getContentType();
            return contentType != null && contentType.toLowerCase().contains("image/gif");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
