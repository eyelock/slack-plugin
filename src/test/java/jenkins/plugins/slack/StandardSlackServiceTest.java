package jenkins.plugins.slack;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_CHANNEL;
import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_THREAD_TS;
import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_TS;
import static jenkins.plugins.slack.SlackResponseTest.assertSlackResponseFail;
import static jenkins.plugins.slack.SlackResponseTest.assertSlackResponseSuccess;

public class StandardSlackServiceTest {
    /**
     * Publish should generally not rethrow exceptions, or it will cause a build job to fail at end.
     */
    @Test
    public void publishWithBadHostShouldNotRethrowExceptions() {
        StandardSlackService service = new StandardSlackService("", "foo", "token", null, false, "#general");
        service.setHost("hostvaluethatwillcausepublishtofail");
        service.publish("message");
    }

    /**
     * Use a valid host, but an invalid team domain
     */
    @Test
    public void invalidTeamDomainShouldFail() {
        StandardSlackService service = new StandardSlackService("", "my", "token", null, false, "#general");
        service.publish("message");
    }

    /**
     * Use a valid team domain, but a bad token
     */
    @Test
    public void invalidTokenShouldFail() {
        StandardSlackService service = new StandardSlackService("", "tinyspeck", "token", null, false, "#general");
        service.publish("message");
    }

    @Test
    public void publishToASingleRoomSendsASingleMessage() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        service.setHttpClient(httpClientStub);
        service.publish("message");
        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
    }

    @Test
    public void publishToMultipleRoomsSendsAMessageToEveryRoom() {
        StandardSlackServiceStub service = new StandardSlackServiceStub("", "domain", "token", null, false, "#room1,#room2,#room3");
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        service.setHttpClient(httpClientStub);
        service.publish("message");
        assertEquals(3, service.getHttpClient().getNumberOfCallsToExecuteMethod());
    }

    @Test
    public void successfulPublishToASingleRoomReturnsSuccess() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));
        SlackResponse response = service.publish("message");
        assertSlackResponseSuccess(response);
    }

    @Test
    public void successfulPublishToMultipleRoomsReturnsSuccess() {
        StandardSlackServiceStub service = new StandardSlackServiceStub("", "domain", "token", null, false, "#room1,#room2,#room3");
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));
        SlackResponse response = service.publish("message");
        assertSlackResponseSuccess(response);
    }

    @Test
    public void failedPublishToASingleRoomReturnsFail() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_NOT_FOUND);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(false));
        SlackResponse response = service.publish("message");
        assertSlackResponseFail(response);
    }

    @Test
    public void singleFailedPublishToMultipleRoomsReturnsFail() {
        StandardSlackServiceStub service = new StandardSlackServiceStub("", "domain", "token", null, false, "#room1,#room2,#room3");
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setFailAlternateResponses(true);
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(false));
        SlackResponse response = service.publish("message");
        assertSlackResponseFail(response);
    }

    @Test
    public void publishToEmptyRoomReturnsSuccess() {
        StandardSlackServiceStub service = new StandardSlackServiceStub("", "domain", "token", null, false, "");
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));
        SlackResponse response = service.publish("message");
        assertSlackResponseSuccess(response);
    }

    @Test
    public void sendAsBotUserReturnsSuccess() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, true, SAMPLE_CHANNEL);
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));
        SlackResponse response = service.publish("message");
        assertSlackResponseSuccess(response);
    }

    @Test
    public void publishNoThreadTsResponseSuccess() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);

        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);

        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true, SAMPLE_CHANNEL, null, SAMPLE_TS));

        SlackResponse response = service.publish("message");

        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
        assertSlackResponseSuccess(response);
    }

    @Test
    public void sendThreadedAsBotUserReturnsSuccess() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, true, SAMPLE_CHANNEL);
        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);
        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));
        SlackResponse response = service.publish("message", "good", SAMPLE_THREAD_TS, false);
        assertSlackResponseSuccess(response, true);
    }

    @Test
    public void publishThreadedToASingleRoomSendsASingleMessage() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);

        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);

        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));

        SlackResponse response = service.publish("message", "good", SAMPLE_THREAD_TS, false);

        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
        assertSlackResponseSuccess(response, true);
    }

    @Test
    public void publishThreadedToMultipeRoomsFails() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, "#room1,#room2,#room3");

        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();

        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(false));

        SlackResponse response = service.publish("message", "good", SAMPLE_THREAD_TS, false);

        assertEquals(0, service.getHttpClient().getNumberOfCallsToExecuteMethod());
        assertSlackResponseFail(response);
    }

    @Test
    public void publishThreadedWithReplyBroadcastSuccess() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);

        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);

        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true));

        SlackResponse response = service.publish("message", "good", SAMPLE_THREAD_TS, true);

        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
        assertSlackResponseSuccess(response, true);
    }

    @Test
    public void publishThreadedNoThreadTsResponseFails() {
        StandardSlackServiceStub service =
                new StandardSlackServiceStub("", "domain", "token", null, false, SAMPLE_CHANNEL);

        CloseableHttpClientStub httpClientStub = new CloseableHttpClientStub();
        httpClientStub.setHttpStatus(HttpStatus.SC_OK);

        service.setHttpClient(httpClientStub);
        service.setResponseString(SlackResponseTest.createSlackResponseString(true, SAMPLE_CHANNEL, null, SAMPLE_TS));

        SlackResponse response = service.publish("message", "good", SAMPLE_THREAD_TS, true);

        assertEquals(1, service.getHttpClient().getNumberOfCallsToExecuteMethod());
        assertSlackResponseFail(response);
    }
}
