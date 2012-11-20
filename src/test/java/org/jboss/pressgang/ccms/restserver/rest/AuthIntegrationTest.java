package org.jboss.pressgang.ccms.restserver.rest;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
@RunWith(Arquillian.class)
public class AuthIntegrationTest extends BaseArquillianIntegrationTest {

    @Test
    @OperateOnDeployment("restServer")
    public void shouldGetResourceMatchingId() {
        // Given a request for category 1 JSON with valid token authorization
        // When the request is made
        // Then the expected JSON for item 1 should be returned
        given().header("Authorization", "Bearer access_token")
                .expect().statusCode(200)
                .and().expect().body("name", equalTo("Audiences"))
                .and().expect().body("sort", equalTo(15))
                .when().get(getBaseTestUrl() + "/1/category/get/json/1");
    }

    @Test
    @OperateOnDeployment("restServer")
    public void shouldFailToGetResourceWhenTokenInvalid() {
        // Given a request with invalid token authorization
        // When the request is made
        // Then an authorization error should be returned
        given().header("Authorization", "Bearer fake_access_token")
                .expect().statusCode(401)
                .when().get(getBaseTestUrl() + "/1/category/get/json/1");
    }

    @Test
    @OperateOnDeployment("restServer")
    public void shouldFailToGetResourceWhenIncorrectScopeForEndpoint() {
        // Given a request with otherwise valid token authorization that doesn't include that endpoint's scope
        // When the request is made
        // Then a forbidden error should be returned
        given().header("Authorization", "Bearer access_token")
                .expect().statusCode(403)
                .when().get(getBaseTestUrl() + "/1/category/delete/json/1");
    }

    @Test
    @OperateOnDeployment("restServer")
    public void shouldFailToGetResourceWhenTokenExpired() {
        // Given a request with otherwise valid token authorization that is past its expiry
        // When the request is made
        // Then an authorization error should be returned
        given().header("Authorization", "Bearer expired_access_token")
                .expect().statusCode(401)
                .when().get(getBaseTestUrl() + "/1/category/get/json/1");
    }

    @Test
    @OperateOnDeployment("restServer")
    public void shouldFailToGetResourceWhenTokenNonCurrent() {
        // Given a request with otherwise valid token authorization that has been marked as non-current
        // When the request is made
        // Then an authorization error should be returned
        given().header("Authorization", "Bearer old_access_token")
                .expect().statusCode(401)
                .when().get(getBaseTestUrl() + "/1/category/get/json/1");
    }

    @Test
    @OperateOnDeployment("restServer")
    public void shouldFailToGetResourceWhenEndpointNotMapped() {
        // Given a request with otherwise valid token authorization to an existing endpoint not mapped to any scope
        // When the request is made
        // Then a bad request error should be returned
        given().header("Authorization", "Bearer access_token")
                .expect().statusCode(400)
                .when().get(getBaseTestUrl() + "/1/user/get/json/1");
    }

}
