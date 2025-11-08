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

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredSetupExtension.class)
public class TransferTest {

    public static Stream<Arguments> amountCorrect() {
        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(200.00),
                Arguments.of(10000.00)
        );
    }

    @DisplayName("Успешный перевод на другой аккаунт")
    @MethodSource("amountCorrect")
    @ParameterizedTest
    public void userCanTransferMoneySuccessfullyParameterized(double transferAmount) {

        // Создание отправителя
        String senderUsername = TestDataFactory.generateValidUsername();
        String senderPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Логин отправителя
        String senderAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        // Создание аккаунта отправителя
        int senderAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // Пополнение баланса
        // Первый депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """.formatted(senderAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Второй депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "id": %d,
                          "balance": 5000
                        }
                        """.formatted(senderAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        // Создание получателя
        String receiverUsername = TestDataFactory.generateValidUsername();
        String receiverPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        // Логин получателя
        String receiverAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        //Создание аккаунта получателя
        int receiverAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", receiverAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        // Совершение перевода
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %s
                        }
                        """.formatted(senderAccountId, receiverAccountId, transferAmount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("message", equalTo("Transfer successful"))
                .body("amount", equalTo((float) transferAmount))
                .body("senderAccountId", equalTo(senderAccountId))
                .body("receiverAccountId", equalTo(receiverAccountId));

        // Проверка транзакций получателя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", receiverAuthHeader)
                .get("http://localhost:4111/api/v1/accounts/" + receiverAccountId + "/transactions")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].amount", equalTo((float) transferAmount))
                .body("[0].type", equalTo("TRANSFER_IN"))
                .body("[0].relatedAccountId", equalTo(senderAccountId));
    }

    public static Stream<Arguments> invalidAmounts() {
        return Stream.of(
                Arguments.of(-0.01, "Transfer amount must be at least 0.01"),
                Arguments.of(10000.1, "Transfer amount cannot exceed 10000")
        );
    }

    @DisplayName("Перевод недопустимой суммы на другой аккаунт")
    @MethodSource("invalidAmounts")
    @ParameterizedTest
    public void userCannotTransferInvalidAmount(double transferAmount, String expectedMessage) {

        //  Создание отправителя
        String senderUsername = TestDataFactory.generateValidUsername();
        String senderPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String senderAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        int senderAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        //  Создание получателя
        String receiverUsername = TestDataFactory.generateValidUsername();
        String receiverPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String receiverAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        int receiverAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", receiverAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        // перевод недопустимой суммы
        String actualResponse = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %s
                        }
                        """.formatted(senderAccountId, receiverAccountId, transferAmount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();

        assertThat(actualResponse, equalTo(expectedMessage));
    }

    @DisplayName("Перевод суммы, превышающей баланс отправителя")
    @Test
    public void transferAmountExceedingBalanceShouldFail() {

        double initialBalance = 50.00;   // Баланс отправителя
        double transferAmount = 100.00;  // Сумма перевода > баланса

        String senderUsername = TestDataFactory.generateValidUsername();
        String senderPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String senderAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(senderUsername, senderPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        int senderAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "id": %d,
                          "balance": %s
                        }
                        """.formatted(senderAccountId, initialBalance))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then().statusCode(HttpStatus.SC_OK);

        //  Создание получателя
        String receiverUsername = TestDataFactory.generateValidUsername();
        String receiverPassword = TestDataFactory.generateValidPassword();

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
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/admin/users")
                .then().statusCode(HttpStatus.SC_CREATED);

        String receiverAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(receiverUsername, receiverPassword))
                .post("http://localhost:4111/api/v1/auth/login")
                .then().statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        int receiverAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", receiverAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then().statusCode(HttpStatus.SC_CREATED)
                .extract().path("id");

        //  Попытка перевода
        String actualResponse = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", senderAuthHeader)
                .body("""
                        {
                          "senderAccountId": %d,
                          "receiverAccountId": %d,
                          "amount": %s
                        }
                        """.formatted(senderAccountId, receiverAccountId, transferAmount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .extract().asString();
        assertThat(actualResponse, containsString("insufficient funds or invalid accounts"));
    }
}