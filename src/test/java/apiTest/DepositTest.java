package apiTest;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utils.RestAssuredSetupExtension;
import utils.TestDataFactory;

import java.util.Locale;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredSetupExtension.class)
public class DepositTest {

    public static Stream<Arguments> amountCorrect() {
        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(200.00),
                Arguments.of(5000.00)
        );
    }

    @DisplayName("Пополнение счета допустимые значения ")
    @MethodSource("amountCorrect")
    @ParameterizedTest
    public void userCanMakeDepositWithCorrectAmountTest(double amount) {
        // генерируем валидные данные
        String username = TestDataFactory.generateValidUsername();
        String password = TestDataFactory.generateValidPassword();

        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создание счета
        int accountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //  депозит на счет
        double balanceDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(String.format(Locale.US, """
                        {
                          "id": %d,
                          "balance": %.2f
                        }
                        """, accountId, amount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .jsonPath()
                .getDouble("balance");

        // Проверяем, что баланс совпадает с депозитом
        assertThat(balanceDeposit, equalTo(amount));
    }

    public static Stream<Arguments> amountInvalid() {
        return Stream.of(
                Arguments.of(-0.01, "Deposit amount must be at least 0.01"),
                Arguments.of(0.0, "Deposit amount must be at least 0.01"),
                Arguments.of(5000.01, "Deposit amount cannot exceed 5000")
        );
    }

    @DisplayName("Пополнение счета не допустимые значения ")
    @MethodSource("amountInvalid")
    @ParameterizedTest
    public void userCanMakeDepositWithInvalidAmountTest(double amount, String expectedErrorMessage) {
        // генерируем валидные данные
        String username = TestDataFactory.generateValidUsername();
        String password = TestDataFactory.generateValidPassword();

        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        // получаем токен юзера
        String userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // создание счета
        int accountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //  депозит на счет
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body("""
                        {
                          "id": %d,
                          "balance": %s
                        }
                        """.formatted(accountId, String.valueOf(amount)))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(equalTo(expectedErrorMessage));
    }

    @DisplayName("Невозможно сделать депозит в чужой аккаунт")
    @Test
    public void userCannotDepositToOtherUserAccountTest() {
        //  Создание первого пользователя
        String username1 = TestDataFactory.generateValidUsername();
        String password1 = TestDataFactory.generateValidPassword();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username1, password1))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String user1Auth = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username1, password1))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //  Создание второго пользователя
        String username2 = TestDataFactory.generateValidUsername();
        String password2 = TestDataFactory.generateValidPassword();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username2, password2))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String user2Auth = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username2, password2))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //  Создание счета второго пользователя
        int user2AccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user2Auth)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        //  Попытка депозита в чужой аккаунт
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user1Auth)
                .body("""
                        {
                          "id": %d,
                          "balance": 100
                        }
                        """.formatted(user2AccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("Unauthorized access to account"));
    }

    @DisplayName("Невозможно сделать депозит в несуществующий аккаунт")
    @Test
    public void userCannotDepositToNonExistentAccountTest() {
        //  Создание пользователя
        String username = TestDataFactory.generateValidUsername();
        String password = TestDataFactory.generateValidPassword();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "USER"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        //  Логин пользователя
        String userAuth = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //  Создание реального аккаунта
        int realAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuth)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        //  Попытка депозита в несуществующий аккаунт
        int nonExistentAccountId = realAccountId + 100; // гарантированно несуществующий
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuth)
                .body("""
                        {
                          "id": %d,
                          "balance": 100
                        }
                        """.formatted(nonExistentAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(equalTo("Unauthorized access to account"));
    }
}
