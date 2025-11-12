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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredSetupExtension.class)
public class ProfileNameTest {

    @DisplayName("Пользователь может изменить имя в профиле на корректное")
    @Test
    public void userCanUpdateProfileNameTest() {
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

        //  Изменение имени
        String newName = "Test Testov";
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuth)
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(newName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("message", equalTo("Profile updated successfully"))
                .body("customer.name", equalTo(newName));

        //  Проверка изменения имени
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuth)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body("name", equalTo(newName));
    }


    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("Test", "Name must consist of two words with letters only"),// 1 слово
                Arguments.of("Test Smith!", "Name must consist of two words with letters only"),// спецсимвол
                Arguments.of(" ", "Name must contain two words with letters only"),// пустая строка
                Arguments.of(" Test", "Name must contain two words with letters only"),// пробел в начале
                Arguments.of("Test  Test", "Name must contain two words with letters only"),// два пробела
                Arguments.of("Test ", "Name must contain two words with letters only")// одно слово + пробел
        );
    }

    @DisplayName("Пользователь не может установить некорректное имя")
    @ParameterizedTest
    @MethodSource("invalidNames")
    public void userCannotUpdateProfileWithInvalidNameTest(String invalidName) {
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

        // обновления профиля с некорректным именем
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuth)
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(invalidName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(containsString("Name must contain two words with letters only"));
    }
}
