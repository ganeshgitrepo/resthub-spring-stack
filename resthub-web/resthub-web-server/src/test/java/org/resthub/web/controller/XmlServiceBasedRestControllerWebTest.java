package org.resthub.web.controller;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.fest.assertions.api.Assertions;
import org.resthub.test.common.AbstractWebTest;
import org.resthub.web.Client;
import org.resthub.web.Http;
import org.resthub.web.Response;
import org.resthub.web.exception.HttpServerErrorException;
import org.resthub.web.exception.NotFoundException;
import org.resthub.web.exception.NotImplementedException;
import org.resthub.web.model.Sample;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class XmlServiceBasedRestControllerWebTest extends AbstractWebTest {

    public XmlServiceBasedRestControllerWebTest() {
         this.activeProfiles = "resthub-web-server,resthub-jpa";
    }

    protected String rootUrl() {
        return "http://localhost:" + port + "/service-based";
    }

    @AfterMethod
    public void tearDown() {
            new Client().url(rootUrl()).delete();
    }

    @Test
    public void testCreateResource() {
        Client httpClient = new Client();
        Sample r = new Sample("toto");
        Response response = httpClient.url(rootUrl()).xmlPost(r);
        r = (Sample) response.resource(r.getClass());
        Assertions.assertThat(r).isNotNull();
        Assertions.assertThat(r.getName()).isEqualTo("toto");
    }
    
    @Test
    public void testFindAllResources() {
        Client httpClient = new Client();
        httpClient.url(rootUrl()).jsonPost(new Sample("toto"));
        httpClient.url(rootUrl()).jsonPost(new Sample("toto"));
        String responseBody = httpClient.url(rootUrl()).getXml().getBody();
        Assertions.assertThat(responseBody).contains("toto");
        Assertions.assertThat(responseBody).contains("<totalElements>2</totalElements>");
        Assertions.assertThat(responseBody).contains("<numberOfElements>2</numberOfElements>");
    }
    
    @Test(expectedExceptions = {NotImplementedException.class})
    public void testFindAllResourcesUnpaginated() {
        Client httpClient = new Client();
        httpClient.url(rootUrl()).jsonPost(new Sample("toto"));
        httpClient.url(rootUrl()).jsonPost(new Sample("toto"));
        Response r = httpClient.url(rootUrl()).setQueryParameter("page", "no").getXml();
        Assertions.assertThat(r).isNotNull();
        Assertions.assertThat(r.getStatus()).isEqualTo(Http.NOT_IMPLEMENTED);
    }
    
    @Test
    public void testFindPaginatedResources() {
        Client httpClient = new Client();
        httpClient.url(rootUrl()).xmlPost(new Sample("toto"));
        httpClient.url(rootUrl()).xmlPost(new Sample("toto"));
        String responseBody = httpClient.url(rootUrl()).setQueryParameter("page", "1").getXml()
                .getBody();
        Assertions.assertThat(responseBody).contains("<totalElements>2</totalElements>");
        Assertions.assertThat(responseBody).contains("<numberOfElements>2</numberOfElements>");
        responseBody = httpClient.url(rootUrl()).setQueryParameter("page", "1")
                .setQueryParameter("size", "1").getXml().getBody();
        Assertions.assertThat(responseBody).contains("<totalElements>2</totalElements>");
        Assertions.assertThat(responseBody).contains("<numberOfElements>1</numberOfElements>");
    }
    
    @Test(expectedExceptions = {HttpServerErrorException.class})
    public void testFindPaginatedResourcesReturnsBadRequestForAnInvalidPageNumber() {
        Client httpClient = new Client();
        httpClient.url(rootUrl()).xmlPost(new Sample("toto"));
        httpClient.url(rootUrl()).xmlPost(new Sample("toto"));
        Response response = httpClient.url(rootUrl()).setQueryParameter("page","0")
                .getXml();
        // TODO: Map IllegalArgumentException to 400 Bad Request
        Assertions.assertThat(response.getStatus()).isEqualTo(500);
        Assertions.assertThat(response.getBody()).contains("Page index must be greater than 0");
    }

    @Test(expectedExceptions = {NotFoundException.class})
    public void testDeleteResource() {
        Client httpClient = new Client();
        Sample r = new Sample("toto");
        r = httpClient.url(rootUrl()).xmlPost(r).resource(r.getClass());
        Assertions.assertThat(r).isNotNull();

        Response response = httpClient.url(rootUrl() + "/" + r.getId()).delete();
        Assertions.assertThat(response.getStatus()).isEqualTo(Http.NO_CONTENT);

        response = httpClient.url(rootUrl() + "/" + r.getId()).get();
        Assertions.assertThat(response.getStatus()).isEqualTo(Http.NOT_FOUND);
    }

    @Test
    public void testFindResource() {
        Client httpClient = new Client();
        Sample r = new Sample("toto");
        r = (Sample) httpClient.url(rootUrl()).xmlPost(r).resource(r.getClass());

        Response response = httpClient.url(rootUrl() + "/" + r.getId()).get();
        Assertions.assertThat(response.getStatus()).isEqualTo(Http.OK);
    }

    @Test
    public void testUpdate() {
        Client httpClient = new Client();
        Sample r1 = new Sample("toto");
        r1 = httpClient.url(rootUrl()).xmlPost(r1).resource(r1.getClass());
        Sample r2 = new Sample(r1);
        r2.setName("titi");
        r2 = httpClient.url(rootUrl() + "/" + r1.getId()).xmlPut(r2).resource(r2.getClass());
        Assertions.assertThat(r1).isNotEqualTo(r2);
        Assertions.assertThat(r1.getName()).contains("toto");
        Assertions.assertThat(r2.getName()).contains("titi");
    }

}
