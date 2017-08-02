package jenkins.plugins.slack.workflow;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.Messages;
import jenkins.plugins.slack.SlackNotifier;
import jenkins.plugins.slack.SlackResponseTest;
import jenkins.plugins.slack.SlackService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_CHANNEL;
import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_THREAD_TS;
import static jenkins.plugins.slack.SlackResponseTest.SAMPLE_TS;
import static jenkins.plugins.slack.SlackResponseTest.assertSlackResponseFail;
import static jenkins.plugins.slack.SlackResponseTest.assertSlackResponseSuccess;

/**
 * Traditional Unit tests, allows testing null Jenkins.getInstance()
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class,SlackSendStep.class})
public class SlackSendStepTest {

    @Mock
    TaskListener taskListenerMock;
    @Mock
    PrintStream printStreamMock;
    @Mock
    PrintWriter printWriterMock;
    @Mock
    StepContext stepContextMock;
    @Mock
    SlackService slackServiceMock;
    @Mock
    Jenkins jenkins;
    @Mock
    SlackNotifier.DescriptorImpl slackDescMock;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Jenkins.class);
        when(jenkins.getDescriptorByType(SlackNotifier.DescriptorImpl.class)).thenReturn(slackDescMock);
    }

    @Test
    public void testStepOverrides() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setToken("token");
        slackSendStep.setTokenCredentialId("tokenCredentialId");
        slackSendStep.setBotUser(false);
        slackSendStep.setBaseUrl("baseUrl/");
        slackSendStep.setTeamDomain("teamDomain");
        slackSendStep.setChannel("channel");
        slackSendStep.setColor("good");
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(slackDescMock.getToken()).thenReturn("differentToken");
        when(slackDescMock.getBotUser()).thenReturn(true);

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(SlackResponseTest.createSlackResponse(true));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response);

        verify(stepExecution, times(1)).getSlackService("baseUrl/", "teamDomain", "token", "tokenCredentialId", false, "channel");
        verify(slackServiceMock, times(1)).publish("message", "good", "", false);
        assertFalse(stepExecution.step.isFailOnError());
    }

    @Test
    public void testStepOverrides2() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setToken("token");
        slackSendStep.setTokenCredentialId("tokenCredentialId");
        slackSendStep.setBotUser(false);
        slackSendStep.setBaseUrl("baseUrl");
        slackSendStep.setTeamDomain("teamDomain");
        slackSendStep.setChannel("channel");
        slackSendStep.setColor("good");
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(slackDescMock.getToken()).thenReturn("differentToken");
        when(slackDescMock.getBotUser()).thenReturn(true);

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(SlackResponseTest.createSlackResponse(true));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response);

        verify(stepExecution, times(1)).getSlackService("baseUrl/", "teamDomain", "token", "tokenCredentialId", false, "channel");
        verify(slackServiceMock, times(1)).publish("message", "good", "", false);
        assertFalse(stepExecution.step.isFailOnError());
    }

    @Test
    public void testValuesForGlobalConfig() throws Exception {

        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        stepExecution.step = new SlackSendStep("message");

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(slackDescMock.getBaseUrl()).thenReturn("globalBaseUrl");
        when(slackDescMock.getTeamDomain()).thenReturn("globalTeamDomain");
        when(slackDescMock.getToken()).thenReturn("globalToken");
        when(slackDescMock.getTokenCredentialId()).thenReturn("globalTokenCredentialId");
        when(slackDescMock.getBotUser()).thenReturn(false);
        when(slackDescMock.getRoom()).thenReturn("globalChannel");

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(SlackResponseTest.createSlackResponse(true));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response, true);

        verify(stepExecution, times(1)).getSlackService("globalBaseUrl", "globalTeamDomain", "globalToken", "globalTokenCredentialId", false, "globalChannel");
        verify(slackServiceMock, times(1)).publish("message", "", "", false);
        assertNull(stepExecution.step.getBaseUrl());
        assertNull(stepExecution.step.getTeamDomain());
        assertNull(stepExecution.step.getToken());
        assertNull(stepExecution.step.getTokenCredentialId());
        assertNull(stepExecution.step.getChannel());
        assertNull(stepExecution.step.getColor());
    }

    @Test
    public void testNonNullEmptyColor() throws Exception {

        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setColor("");
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(SlackResponseTest.createSlackResponse(true));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response);

        verify(slackServiceMock, times(1)).publish("message", "", "", false);
        assertNull(stepExecution.step.getColor());
    }

    @Test
    public void testNullJenkinsInstance() throws Exception {

        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        stepExecution.step = new SlackSendStep("message");

        when(Jenkins.getInstance()).thenThrow(NullPointerException.class);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.error(anyString())).thenReturn(printWriterMock);
        doNothing().when(printStreamMock).println();

        Map<String, String> response = stepExecution.run();
        // Jenkins failure the current behaviour is to return null, it does not return false
        assertNull(response);

        verify(taskListenerMock, times(1)).error(Messages.NotificationFailedWithException(anyString()));
    }
    
    @Test
    public void testThreadedSendNoReplyBroadcast() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setThreadTs(SlackResponseTest.SAMPLE_THREAD_TS);
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), eq(SAMPLE_THREAD_TS), eq(false)))
                .thenReturn(SlackResponseTest.createSlackResponse(true, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response, true);

        verify(slackServiceMock, times(1)).publish("message", "", SAMPLE_THREAD_TS, false);
    }

    @Test
    public void testThreadedSendWithReplyBroadcast() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setThreadTs(SlackResponseTest.SAMPLE_THREAD_TS);
        slackSendStep.setReplyBroadcast(true);
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), eq(SAMPLE_THREAD_TS), eq(true)))
                .thenReturn(SlackResponseTest.createSlackResponse(true, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseSuccess(response, true);

        verify(slackServiceMock, times(1)).publish("message", "", SAMPLE_THREAD_TS, true);
    }

    @Test
    public void testEmptyMapResponseOnFail() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setThreadTs(SlackResponseTest.SAMPLE_THREAD_TS);
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), eq(SAMPLE_THREAD_TS), eq(false)))
                .thenReturn(SlackResponseTest.createSlackResponse(false, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseFail(response);

        verify(slackServiceMock, times(1)).publish("message", "", SAMPLE_THREAD_TS, false);
    }

    @Test
    public void testThreadedNoThreadTsFails() throws Exception {
        SlackSendStep.SlackSendStepExecution stepExecution = spy(new SlackSendStep.SlackSendStepExecution());
        SlackSendStep slackSendStep = new SlackSendStep("message");
        slackSendStep.setThreadTs(SlackResponseTest.SAMPLE_THREAD_TS);
        slackSendStep.setChannel(SlackResponseTest.SAMPLE_CHANNEL);
        stepExecution.step = slackSendStep;

        when(Jenkins.getInstance()).thenReturn(jenkins);

        stepExecution.listener = taskListenerMock;

        when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
        doNothing().when(printStreamMock).println();

        when(slackServiceMock.publish(anyString(), anyString(), eq(SAMPLE_THREAD_TS), eq(false)))
                .thenReturn(SlackResponseTest.createSlackResponse(false, SAMPLE_CHANNEL, null, SAMPLE_TS));
        when(stepExecution.getSlackService(anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString())).thenReturn(slackServiceMock);

        Map<String, String> response = stepExecution.run();
        assertSlackResponseFail(response);

        verify(slackServiceMock, times(1)).publish("message", "", SAMPLE_THREAD_TS, false);
    }
}
