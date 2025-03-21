package com.wjq.wjqpicturebackend.utils.color;

import com.wjq.wjqpicturebackend.exception.BusinessException;
import com.wjq.wjqpicturebackend.exception.ErrorCode;

/**
 * 处理16进制颜色
 */
public class HexColorFixer {

    public static String fixHexColor(String hexColor) {
        // 去掉前缀
        if (hexColor.startsWith("0x") || hexColor.startsWith("0X")) {
            hexColor = hexColor.substring(2);
        } else if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }

        // 如果长度是3，表示每个字符代表两个相同的16进制数字
        if (hexColor.length() == 3) {
            StringBuilder fullHexColor = new StringBuilder();
            for (char c : hexColor.toCharArray()) {
                fullHexColor.append(c).append(c);
            }
            return "0x" + fullHexColor.toString();
        }

        // 如果长度是4或5，表示有部分重复被省略  
        if (hexColor.length() == 4 || hexColor.length() == 5) {
            StringBuilder fullHexColor = new StringBuilder();
            for (int i = 0; i < hexColor.length(); i++) {
                char c = hexColor.charAt(i);
                fullHexColor.append(c);
                if (i < hexColor.length() - 1 && hexColor.charAt(i) == hexColor.charAt(i + 1)) {
                    fullHexColor.append(c);
                }
            }
            return "0x" + fullHexColor.toString();
        }

        // 如果长度已经是6，直接返回
        if (hexColor.length() == 6) {
            return "0x" + hexColor;
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR);
    }

    public static void main(String[] args) {
        String hexColor = "0x4456";
        String fixedHexColor = fixHexColor(hexColor);
        System.out.println("Fixed hex color: " + fixedHexColor);
    }
}