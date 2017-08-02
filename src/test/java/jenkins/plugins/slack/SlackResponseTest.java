package jenkins.plugins.slack;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SlackResponseTest {

    public static final String SAMPLE_THREAD_TS = "1482960137.003543";
    public static final String SAMPLE_TS = "1483116860.020084";
    public static final String SAMPLE_CHANNEL = "#room1";

    public static void assertSlackResponseSuccess(Map<String, String> response) {
        assertSlackResponseSuccess(response, false);
    }

    public static void assertSlackResponseSuccess(Map<String, String> response, boolean isThreaded) {
        assertTrue(response != null);
        assertFalse(response.isEmpty());
        assertTrue(Boolean.parseBoolean(response.get(SlackResponse.SUCCESS_KEY)));
        assertEquals(SAMPLE_CHANNEL, response.get(SlackResponse.CHANNEL_KEY));
        assertEquals(SAMPLE_TS, response.get(SlackResponse.TS_KEY));

        if (isThreaded) {
            assertEquals(SAMPLE_THREAD_TS, response.get(SlackResponse.THREAD_TS_KEY));
        }
    }

    public static void assertSlackResponseSuccess(SlackResponse response) {
        assertSlackResponseSuccess(response, false);
    }

    public static void assertSlackResponseSuccess(SlackResponse response, boolean isThreaded) {
        assertTrue(response != null);
        assertTrue(response.isSuccess());
        assertEquals(SAMPLE_CHANNEL, response.getChannel());
        assertEquals(SAMPLE_TS, response.getTs());

        if (isThreaded) {
            assertEquals(SAMPLE_THREAD_TS, response.getThreadTs());
        }
    }

    public static void assertSlackResponseFail(Map<String, String> response) {
        assertTrue(response != null);
        assertTrue(response.isEmpty());
    }

    public static void assertSlackResponseFail(SlackResponse response) {
        assertTrue(response != null);
        assertTrue(!response.isSuccess());
    }

    public static SlackResponse createMockSlackResponse(boolean success) {
        if (success) {
            return createMockSlackResponse(success, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS);
        } else {
            return createMockSlackResponse(success, null, null, null);
        }
    }

    public static SlackResponse createMockSlackResponse(boolean success, String channel, String threadTs, String ts) {
        SlackResponse mockSlackResponse = mock(SlackResponse.class);
        when(mockSlackResponse.isSuccess()).thenReturn(success);
        when(mockSlackResponse.getChannel()).thenReturn(channel);
        when(mockSlackResponse.getThreadTs()).thenReturn(threadTs);
        when(mockSlackResponse.getTs()).thenReturn(ts);
        return mockSlackResponse;
    }

    public static SlackResponse createSlackResponse(boolean success) {
        if (success) {
            return createSlackResponse(success, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS);
        } else {
            return createSlackResponse(success, null, null, null);
        }
    }

    public static SlackResponse createSlackResponse(boolean success, String channel, String threadTs, String ts) {
        SlackResponse slackResponse = new SlackResponse();
        slackResponse.setSuccess(success);
        slackResponse.setChannel(channel);
        slackResponse.setThreadTs(threadTs);
        slackResponse.setTs(ts);
        return slackResponse;
    }

    public static String createSlackResponseString(boolean success) {
        if (success) {
            return createSlackResponseString(success, SAMPLE_CHANNEL, SAMPLE_THREAD_TS, SAMPLE_TS);
        } else {
            return createSlackResponseString(success, null, null, null);
        }
    }

    public static String createSlackResponseString(boolean success, String channel, String threadTs, String ts) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"ok\": " + success + ",");
        builder.append("\"channel\": \"" + (channel == null ? "" : channel) + "\",");
        builder.append("\"ts\": \"" + (ts == null ? "" : ts) + "\",");
        builder.append("\"message\": {");
        builder.append("\"thread_ts\": \"" + (threadTs == null ? "" : threadTs) + "\"");
        builder.append("}");
        builder.append("}");
        return builder.toString();
    }
}
