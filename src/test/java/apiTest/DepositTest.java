package apiTest;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositRequester;
import requests.LoginUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.notNullValue;


public class DepositTest extends BaseTest {

    public static Stream<Arguments> amountCorrect() {
        return Stream.of(
                Arguments.of(0.01),
                Arguments.of(200.00),
                Arguments.of(5000.00)
        );
    }

    @DisplayName("Пополнение счета допустимые значения")
    @MethodSource("amountCorrect")
    @ParameterizedTest
    public void userCanMakeDepositWithCorrectAmountTest(double amount) {

        //  Создаем пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        //  Аутентификация пользователя
        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder()
                        .username(userRequest.getUsername())
                        .password(userRequest.getPassword())
                        .build())
                .header("Authorization", notNullValue());

        //  Создаем счет для пользователя
        int accountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()), // исправлено
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .path("id");

        // Пополняем счет и получаем типизированный ответ
        DepositResponse response = new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(DepositRequest.builder()
                        .id(accountId)
                        .balance(amount)
                        .build())
                .extract()
                .as(DepositResponse.class);

        softly.assertThat(response.getBalance()).isEqualTo(amount);
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

        // Создаём пользователя через API
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // Создаём счёт
        int accountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .path("id");

        // Пытаемся внести недопустимую сумму
        new DepositRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestPlainText(expectedErrorMessage))
                .post(DepositRequest.builder()
                        .id(accountId)
                        .balance(amount)
                        .build());
    }

    @DisplayName("Невозможно сделать депозит в чужой аккаунт")
    @Test
    public void userCannotDepositToOtherUserAccountTest() {
        // Создаём первого пользователя
        CreateUserRequest user1 = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user1);

        // Создаём второго пользователя
        CreateUserRequest user2 = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user2);

        // Создаём аккаунт у второго пользователя
        int user2AccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(user2.getUsername(), user2.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .path("id");

        // Пытаемся сделать депозит в чужой аккаунт
        new DepositRequester(
                RequestSpecs.authAsUser(user1.getUsername(), user1.getPassword()),
                ResponseSpecs.requestReturnsForbiddenPlainText("Unauthorized access to account"))
                .post(DepositRequest.builder()
                        .id(user2AccountId)
                        .balance(100)
                        .build());
    }

    @DisplayName("Невозможно сделать депозит в несуществующий аккаунт")
    @Test
    public void userCannotDepositToNonExistentAccountTest() {
        // Создаём пользователя
        CreateUserRequest user = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user);

        // Создаём аккаунт
        int realAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .path("id");

        // Пытаемся внести депозит в несуществующий аккаунт
        int fakeAccountId = realAccountId + 100;
        new DepositRequester(
                RequestSpecs.authAsUser(user.getUsername(), user.getPassword()),
                ResponseSpecs.requestReturnsForbiddenPlainText("Unauthorized access to account"))
                .post(DepositRequest.builder()
                        .id(fakeAccountId)
                        .balance(100)
                        .build());
    }
}

