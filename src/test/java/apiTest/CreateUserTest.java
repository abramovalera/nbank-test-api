package apiTest;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
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

@ExtendWith(RestAssuredSetupExtension.class)
public class CreateUserTest {

    @Test
    public void adminCanCreateUserWithCorrectData() {
        String username = TestDataFactory.generateValidUsername();
        String password = TestDataFactory.generateValidPassword();
        String role = "USER";

        String requestBody = String.format("""
                {
                  "username": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """, username, password, role);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo(username)) // проверяем, что вернулся именно наш username
                .body("password", Matchers.not(Matchers.equalTo(password))) // пароль не должен храниться в открытом виде
                .body("role", Matchers.equalTo(role));
    }


    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                // username field validation
                Arguments.of("", "Password11!", "USER", "username", "Username cannot be blank"),
                Arguments.of("us", "Password11!", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("use r", "Password11!", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("use@r", "Password11!", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"));
    }

    @DisplayName("Создание пользователя с некорректным username")
    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotUserWithInvalidData(String username, String password, String role, String errorKey, String errorValue) {
        String requestBody = String.format(
                """
                        {
                          "username": "%s",
                          "password": "%s",
                          "role": "%s"
                        }
                        """, username, password, role);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(errorKey, Matchers.hasItem(errorValue));
    }
}
