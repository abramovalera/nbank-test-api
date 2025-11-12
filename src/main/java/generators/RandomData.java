package generators;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomData {
    private RandomData() {
    }

    public static String getUsername() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getPassword() {
        // Заглавные буквы
        String upper = RandomStringUtils.randomAlphabetic(2).toUpperCase();
        // Строчные буквы
        String lower = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        // Цифры
        String digits = RandomStringUtils.randomNumeric(2);
        // Спецсимволы
        String special = "%#@";

        return upper + lower + digits + special;
    }
}
