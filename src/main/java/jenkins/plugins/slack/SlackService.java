package jenkins.plugins.slack;

public interface SlackService {
    SlackResponse publish(String message);

    SlackResponse publish(String message, String color);

    SlackResponse publish(String message, String color, String threadTs, boolean replyBroadcast);
}
