package jenkins.plugins.slack;

import java.io.Serializable;

public class SlackResponse implements Serializable {
    private static final long serialVersionUID = -3124103784297767955L;

    public static final String SUCCESS_KEY = "success";
    public static final String CHANNEL_KEY = "channel";
    public static final String THREAD_TS_KEY = "threadTs";
    public static final String TS_KEY = "ts";

    private boolean success;
    private String channel;
    private String threadTs;
    private String ts;

    public SlackResponse() {
        this.success = false;
        this.channel = null;
        this.threadTs = null;
        this.ts = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getThreadTs() {
        return threadTs;
    }

    public void setThreadTs(String threadTs) {
        this.threadTs = threadTs;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }
}
