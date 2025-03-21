package com.wjq.wjqpicturebackend.utils.color;

import java.awt.*;

public class ColorSimilarUtils {

    public static double calculateSimilarity(Color color1, Color color2) {
        int red1 = color1.getRed();
        int green1 = color1.getGreen();
        int blue1 = color1.getBlue();
        int red2 = color2.getRed();
        int green2 = color2.getGreen();
        int blue2 = color2.getBlue();
        return Math.sqrt(Math.pow(red2-red1, 2) + Math.pow(green2-green1, 2) + Math.pow(blue2-blue1, 2));
    }
}
