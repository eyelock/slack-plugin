package jenkins.plugins.slack;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.Descriptor;
import hudson.util.FormValidation;
import junit.framework.TestCase;

@RunWith(Parameterized.class)
public class SlackNotifierTest extends TestCase {

    private SlackNotifierStub.DescriptorImplStub descriptor;
    private SlackServiceStub slackServiceStub;
    private SlackResponse response;
    private FormValidation.Kind expectedResult;

    @Rule
    public final JenkinsRule rule = new JenkinsRule();

    @Before
    @Override
    public void setUp() {
        descriptor = new SlackNotifierStub.DescriptorImplStub();
    }

    public SlackNotifierTest(SlackServiceStub slackServiceStub, SlackResponse response,
            FormValidation.Kind expectedResult) {
        this.slackServiceStub = slackServiceStub;
        this.response = response;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection businessTypeKeys() {
        return Arrays.asList(new Object[][]{
                {
                        new SlackServiceStub(), SlackResponseTest.createSlackResponse(true),
                        FormValidation.Kind.OK
                }, {
                        new SlackServiceStub(), SlackResponseTest.createSlackResponse(false),
                        FormValidation.Kind.ERROR
                },
                {
                        null, SlackResponseTest.createSlackResponse(false),
                        FormValidation.Kind.ERROR
                }
        });
    }

    @Test
    public void testDoTestConnection() {
        if (slackServiceStub != null) {
            slackServiceStub.setResponse(response);
        }
        descriptor.setSlackService(slackServiceStub);
        try {
            FormValidation result = descriptor.doTestConnection("baseUrl", "teamDomain", "authToken", "authTokenCredentialId", false,"room");
            assertEquals(result.kind, expectedResult);
        } catch (Descriptor.FormException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public static class SlackServiceStub implements SlackService {

        private SlackResponse response;

        @Override
        public SlackResponse publish(String message) {
            return response;
        }

        @Override
        public SlackResponse publish(String message, String color) {
            return response;
        }

        @Override
        public SlackResponse publish(String message, String color, String threadTs, boolean replyBroadcast) {
            return response;
        }

        public void setResponse(SlackResponse response) {
            this.response = response;
        }
    }
}
