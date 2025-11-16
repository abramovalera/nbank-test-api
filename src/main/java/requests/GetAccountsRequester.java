package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class GetAccountsRequester extends Request {

    public GetAccountsRequester(RequestSpecification requestSpecification,
                                ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(models.BaseModel model) {
        throw new UnsupportedOperationException("GET-запрос не использует BaseModel");
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}
