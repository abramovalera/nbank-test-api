package utils;

import java.security.SecureRandom;

public class TestDataFactory {
    private static final SecureRandom random = new SecureRandom();
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&+=";

    // Генерация  username
    public static String generateValidUsername() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_.-";
        int len = 5 + random.nextInt(10); // от 5 до 14 символов
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }

    // Генерация  пароля
    public static String generateValidPassword() {
        StringBuilder sb = new StringBuilder();
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        // Остальные символы заполняем случайными из всех разрешённых
        String all = UPPER + LOWER + DIGITS + SPECIAL;
        for (int i = 4; i < 10; i++) { // длина пароля = 10
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        // Перемешиваем, чтобы первые 4 символа не всегда одни и те же
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return new String(arr);
    }
}
