package iteration.api;

import generators.RandomData;
import models.AccountResponse;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.GetCustomerAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

public class CreateAccountTest extends BaseTest {

    @DisplayName("Пользователь может создать аккаунт")
    @Test
    public void userCanCreateAccountTest() {

        // Создаём нового пользователя
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(userRequest);

        // создаём аккаунт под этим пользователем
        int createdAccountId = new CreateAccountRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .path("id");

        // Получаем все аккаунты пользователя
        List<AccountResponse> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(userRequest.getUsername(), userRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .jsonPath()
                .getList(".", AccountResponse.class);

        // Проверяем, что созданный аккаунт есть в списке
        softly.assertThat(accounts)
                .extracting(AccountResponse::getId)
                .contains((long) createdAccountId);
    }
}
