package com.lootlookup.utils;

import lombok.extern.slf4j.Slf4j;

import net.runelite.client.RuneLite;
import okhttp3.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Consumer;

import static com.lootlookup.utils.Icons.noteImg;

@Slf4j
public class Util {
    public static void downloadImage(OkHttpClient okHttpClient, String url, Consumer<BufferedImage> callback) {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
            .header("Referer", "https://oldschool.runescape.wiki/")
            .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Failed to download image", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        return;
                    }
                    
                    try (InputStream is = responseBody.byteStream()) {
                        BufferedImage image = ImageIO.read(is);
                        if (image != null) {
                            SwingUtilities.invokeLater(() -> callback.accept(image));
                        }
                    }
                }
            }
        });
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

    public static int getStringWidth(Font font, String text) {
        FontMetrics fm = new JLabel().getFontMetrics(font);
        return fm.stringWidth(text);
    }

    public static String fitText(Font font, String[] candidates, int availableWidth) {
        FontMetrics fm = new JLabel().getFontMetrics(font);
        for (String text : candidates) {
            if (fm.stringWidth(text) <= availableWidth) {
                return text;
            }
        }
        return candidates[candidates.length - 1];
    }

    public static String truncateToFit(Font font, String text, int availableWidth) {
        FontMetrics fm = new JLabel().getFontMetrics(font);
        if (fm.stringWidth(text) <= availableWidth) return text;

        String ellipsis = "\u2026";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        for (int i = text.length() - 1; i > 0; i--) {
            if (fm.stringWidth(text.substring(0, i)) + ellipsisWidth <= availableWidth) {
                return text.substring(0, i) + ellipsis;
            }
        }
        return ellipsis;
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

    /**
     * Scales a GP value into a consistent value-based label for the list view:
     * values under 10,000 show with full comma-separated precision and a
     * "gp" suffix, 10k-999k show as "NK", 1M-999M as "NM", and 1B and above
     * as "NB", matching the in-game stack shorthand. Trailing ".0" is
     * suppressed so round values render as "10K" and "1M" rather than
     * "10.0K" / "1.0M". Values that round up to the next tier at the
     * boundary (e.g. 999,950 would otherwise format as "1000K") get promoted
     * to the next tier so they render as "1M".
     */
    public static String rsScaledPrice(long value) {
        if (value < 0) {
            return "-" + rsScaledPrice(-value);
        }
        if (value < 10_000) {
            return NumberFormat.getNumberInstance().format(value) + "gp";
        }
        long[] divisors = {1_000L, 1_000_000L, 1_000_000_000L};
        String[] suffixes = {"K", "M", "B"};
        int tier;
        if (value >= divisors[2]) {
            tier = 2;
        } else if (value >= divisors[1]) {
            tier = 1;
        } else {
            tier = 0;
        }
        long roundedTenths = Math.round(value * 10.0 / divisors[tier]);
        // If rounding carries to 1000.0 at this tier, promote to the next
        if (roundedTenths >= 10_000 && tier < divisors.length - 1) {
            tier++;
            roundedTenths = Math.round(value * 10.0 / divisors[tier]);
        }
        double scaled = roundedTenths / 10.0;
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(scaled) + suffixes[tier];
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