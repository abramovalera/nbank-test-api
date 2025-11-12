package apiTest;

import generators.RandomData;
import models.CreateUserRequest;
import models.UpdateProfileRequest;
import models.UpdateProfileResponse;
import models.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.UpdateProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class ProfileNameTest extends BaseTest {

    @DisplayName("Пользователь может изменить имя в профиле на корректное")
    @Test
    public void userCanUpdateProfileNameTest() {
        // Создание пользователя
        CreateUserRequest user = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user);

        // Авторизация
        var userSpec = RequestSpecs.authAsUser(user.getUsername(), user.getPassword());

        // Изменение имени
        String newName = "Test Testov";
        UpdateProfileResponse response = new UpdateProfileRequester(
                userSpec,
                ResponseSpecs.requestReturnsOKWithMessage("Profile updated successfully"))
                .put(UpdateProfileRequest.builder().name(newName).build())
                .extract().as(UpdateProfileResponse.class);

        // Проверки
        softly.assertThat(response.getCustomer().getName()).isEqualTo(newName);
        softly.assertThat(response.getMessage()).isEqualTo("Profile updated successfully");

        // Проверка профиля GET-запросом
        new UpdateProfileRequester(
                userSpec,
                ResponseSpecs.requestReturnsOK())
                .getProfile()
                .body("name", org.hamcrest.Matchers.equalTo(newName));
    }

    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("Test", "Name must consist of two words with letters only"),
                Arguments.of("Test Smith!", "Name must consist of two words with letters only"),
                Arguments.of(" ", "Name must contain two words with letters only"),
                Arguments.of(" Test", "Name must contain two words with letters only"),
                Arguments.of("Test  Test", "Name must contain two words with letters only"),
                Arguments.of("Test ", "Name must contain two words with letters only")
        );
    }

    @DisplayName("Пользователь не может установить некорректное имя")
    @ParameterizedTest
    @MethodSource("invalidNames")
    public void userCannotUpdateProfileWithInvalidNameTest(String invalidName, String expectedMessage) {
        // Создание пользователя
        CreateUserRequest user = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(user);

        // Авторизация
        var userSpec = RequestSpecs.authAsUser(user.getUsername(), user.getPassword());

        // Попытка обновления имени
        new UpdateProfileRequester(
                userSpec,
                ResponseSpecs.requestReturnsBadRequestPlainTextContaining(expectedMessage))
                .put(UpdateProfileRequest.builder().name(invalidName).build());
    }
}
