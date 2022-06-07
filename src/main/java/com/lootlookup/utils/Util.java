package com.lootlookup.utils;

import com.lootlookup.osrswiki.WikiItem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Consumer;

import static com.lootlookup.utils.Icons.noteImg;

public class Util {
    public static void downloadImage(String url, Consumer<BufferedImage> callback) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
            connection.connect();
            BufferedImage image = ImageIO.read(connection.getInputStream());
            connection.disconnect();

            callback.accept(image);
        } catch (IOException e) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static BufferedImage resizeImg(BufferedImage img, int newW, int newH) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = new BufferedImage(newW, newH, img.getType());
        Graphics2D g = dimg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null);
        g.dispose();
        return dimg;
    }

    public static BufferedImage resizeImgPerc(Object img, int percent) {
        BufferedImage buff = (BufferedImage) img;
        return resize(buff, buff.getWidth() * percent / 100, buff.getHeight() * percent / 100);
    }

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }

    public static BufferedImage getNotedImg(BufferedImage image) {
        BufferedImage target = new BufferedImage(noteImg.getWidth(), noteImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) target.getGraphics();
        g.drawImage(noteImg, 0, 0, null);

        int drawX = ((noteImg.getWidth()  - image.getWidth()) / 2) + (image.getWidth() / 7);
        int drawY = ((noteImg.getHeight()  - image.getHeight()) / 2) + (image.getHeight() / 7);

        g.drawImage(Util.resizeImgPerc(image, 70), drawX, drawY, null);
        return target;
    }

    public static void showHandCursorOnHover(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                evt.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                evt.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    public static String colorToHex(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    public static String rsFormat(double number) {
        int power;
        String suffix = " KMBT";
        String formattedNumber = "";

        NumberFormat formatter = new DecimalFormat("#,###.#");
        power = (int) StrictMath.log10(number);
        number = number / (Math.pow(10, (power / 3) * 3));
        formattedNumber = formatter.format(number);
        formattedNumber = formattedNumber + suffix.charAt(power / 3);
        return formattedNumber.length() > 4 ? formattedNumber.replaceAll("\\.[0-9]+", "") : formattedNumber;
    }

    public static String toPercentage(double n, int digits) {
        return String.format("%." + digits + "f", n * 100) + "%";
    }

    public static String convertDecimalToFraction(double x) {
        if (x < 0) {
            return "-" + convertDecimalToFraction(-x);
        }

        double tolerance = 1.0E-6;
        double h1 = 1;
        double h2 = 0;
        double k1 = 0;
        double k2 = 1;
        double b = x;
        do {
            double a = Math.floor(b);
            double aux = h1;
            h1 = a * h1 + h2;
            h2 = aux;
            aux = k1;
            k1 = a * k1 + k2;
            k2 = aux;
            b = 1 / (b - a);
        } while (Math.abs(x - h1 / k1) > x * tolerance);

        int h1Int = (int) h1;
        int k1Int = (int) k1;

        double denom = k1 / h1;

        int denomInt = k1Int / h1Int;

        String denomStr = String.valueOf(Math.round(denom * 100.0) / 100.0);
        if (Math.floor(denom) == denom) {
            denomStr = String.valueOf(denomInt);
        }

        return 1 + "/" + denomStr;
    }
}
