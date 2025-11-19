package iteration.api;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class TransferTest extends BaseTest {

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
    public void userCanTransferMoneyTest(double transferAmount) {

        // Создаём отправителя
        CreateUserRequest sender = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(sender);

        // Создаём аккаунт отправителя
        int senderAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Пополняем счёт отправителя дважды по 5000
        DepositRequester depositRequester = new DepositRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.requestReturnsOK());

        depositRequester.post(DepositRequest.builder().id(senderAccountId).balance(5000).build());
        depositRequester.post(DepositRequest.builder().id(senderAccountId).balance(5000).build());

        // Создаём получателя
        CreateUserRequest receiver = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(receiver);

        // Создаём аккаунт получателя
        int receiverAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(receiver.getUsername(), receiver.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Совершаем перевод
        TransferResponse response = new TransferRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.requestReturnsOKWithMessage("Transfer successful"))
                .post(TransferRequest.builder()
                        .senderAccountId(senderAccountId)
                        .receiverAccountId(receiverAccountId)
                        .amount(transferAmount)
                        .build())
                .extract()
                .as(TransferResponse.class);

        softly.assertThat(response.getAmount()).isEqualTo(transferAmount);
        softly.assertThat(response.getSenderAccountId()).isEqualTo(senderAccountId);
        softly.assertThat(response.getReceiverAccountId()).isEqualTo(receiverAccountId);
        softly.assertThat(response.getMessage()).isEqualTo("Transfer successful");
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
    public void userCannotTransferInvalidAmountTest(double transferAmount, String expectedMessage) {

        // Создаём отправителя
        CreateUserRequest sender = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(sender);

        int senderAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Создаём получателя
        CreateUserRequest receiver = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(receiver);

        int receiverAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(receiver.getUsername(), receiver.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Пытаемся перевести недопустимую сумму
        new TransferRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.requestReturnsBadRequestPlainText(expectedMessage))
                .post(TransferRequest.builder()
                        .senderAccountId(senderAccountId)
                        .receiverAccountId(receiverAccountId)
                        .amount(transferAmount)
                        .build());
    }

    @DisplayName("Перевод суммы, превышающей баланс отправителя")
    @Test
    public void transferAmountExceedingBalanceShouldFailTest() {

        double initialBalance = 50.00;
        double transferAmount = 100.00;

        // Создаём отправителя
        CreateUserRequest sender = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(sender);

        int senderAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Пополняем баланс (меньше, чем перевод)
        new DepositRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(DepositRequest.builder()
                        .id(senderAccountId)
                        .balance(initialBalance)
                        .build());

        // Создаём получателя
        CreateUserRequest receiver = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(receiver);

        int receiverAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(receiver.getUsername(), receiver.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract().path("id");

        // Пытаемся перевести сумму больше баланса
        new TransferRequester(
                RequestSpecs.authAsUser(sender.getUsername(), sender.getPassword()),
                ResponseSpecs.requestReturnsBadRequestPlainTextContaining("insufficient funds or invalid accounts"))
                .post(TransferRequest.builder()
                        .senderAccountId(senderAccountId)
                        .receiverAccountId(receiverAccountId)
                        .amount(transferAmount)
                        .build());
    }
}
