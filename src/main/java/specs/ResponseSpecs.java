package specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;

public class ResponseSpecs {
    private ResponseSpecs() {}

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    // 200 / 201
    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification requestReturnsOKWithMessage(String expectedMessage) {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("message", equalTo(expectedMessage))
                .build();
    }

    // 400
    public static ResponseSpecification requestReturnsBadRequest(String errorKey, String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequestPlainText(String expectedMessage) {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(equalTo(expectedMessage))
                .build();
    }

    //проверяет подстроку
    public static ResponseSpecification requestReturnsBadRequestPlainTextContaining(String expectedPart) {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(containsString(expectedPart))
                .build();
    }

    //  403
    public static ResponseSpecification requestReturnsForbiddenPlainText(String expectedMessage) {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(equalTo(expectedMessage))
                .build();
    }
}
