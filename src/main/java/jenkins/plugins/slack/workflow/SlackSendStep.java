package jenkins.plugins.slack.workflow;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.Project;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.plugins.slack.Messages;
import jenkins.plugins.slack.SlackNotifier;
import jenkins.plugins.slack.SlackResponse;
import jenkins.plugins.slack.SlackService;
import jenkins.plugins.slack.StandardSlackService;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Workflow step to send a Slack channel notification.
 */
public class SlackSendStep extends AbstractStepImpl {

    private final @Nonnull String message;
    private String color;
    private String token;
    private String tokenCredentialId;
    private boolean botUser;
    private String channel;
    private String baseUrl;
    private String teamDomain;
    private boolean failOnError;
    private String threadTs;
    private boolean replyBroadcast;


    @Nonnull
    public String getMessage() {
        return message;
    }

    public String getColor() {
        return color;
    }

    @DataBoundSetter
    public void setColor(String color) {
        this.color = Util.fixEmpty(color);
    }

    public String getToken() {
        return token;
    }

    @DataBoundSetter
    public void setToken(String token) {
        this.token = Util.fixEmpty(token);
    }

    public String getTokenCredentialId() {
        return tokenCredentialId;
    }

    @DataBoundSetter
    public void setTokenCredentialId(String tokenCredentialId) {
        this.tokenCredentialId = Util.fixEmpty(tokenCredentialId);
    }

    public boolean getBotUser() {
        return botUser;
    }

    @DataBoundSetter
    public void setBotUser(boolean botUser) {
        this.botUser = botUser;
    }

    public String getChannel() {
        return channel;
    }

    @DataBoundSetter
    public void setChannel(String channel) {
        this.channel = Util.fixEmpty(channel);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @DataBoundSetter
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = Util.fixEmpty(baseUrl);
        if(this.baseUrl != null && !this.baseUrl.isEmpty() && !this.baseUrl.endsWith("/")) {
            this.baseUrl += "/";
        }
    }

    public String getTeamDomain() {
        return teamDomain;
    }

    @DataBoundSetter
    public void setTeamDomain(String teamDomain) {
        this.teamDomain = Util.fixEmpty(teamDomain);
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundSetter
    public void setThreadTs(String threadTs) {
        this.threadTs = Util.fixEmpty(threadTs);
    }

    public String getThreadTs() {
        return threadTs;
    }

    @DataBoundSetter
    public void setReplyBroadcast(boolean replyBroadcast) {
        this.replyBroadcast = replyBroadcast;
    }

    public boolean getReplyBroadcast() {
        return replyBroadcast;
    }

    @DataBoundConstructor
    public SlackSendStep(@Nonnull String message) {
        this.message = message;
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(SlackSendStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "slackSend";
        }

        @Override
        public String getDisplayName() {
            return Messages.SlackSendStepDisplayName();
        }

        public ListBoxModel doFillTokenCredentialIdItems(@AncestorInPath Project project) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new ListBoxModel();
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(lookupCredentials(
                            StringCredentials.class,
                            project,
                            ACL.SYSTEM,
                            new HostnameRequirement("*.slack.com"))
                    );
        }

        //WARN users that they should not use the plain/exposed token, but rather the token credential id
        public FormValidation doCheckToken(@QueryParameter String value) {
            //always show the warning - TODO investigate if there is a better way to handle this
            return FormValidation.warning("Exposing your Integration Token is a security risk. Please use the Integration Token Credential ID");
        }
    }

    public static class SlackSendStepExecution
            extends AbstractSynchronousNonBlockingStepExecution<Map<String, String>> {

        private static final long serialVersionUID = 1L;

        @Inject
        transient SlackSendStep step;

        @StepContextParameter
        transient TaskListener listener;

        @Override
        protected Map<String, String> run() throws Exception {

            //default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            //Jenkins.getInstance() may return null, no message sent in that case
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                listener.error(Messages.NotificationFailedWithException(ne));
                return null;
            }
            SlackNotifier.DescriptorImpl slackDesc = jenkins.getDescriptorByType(SlackNotifier.DescriptorImpl.class);
            listener.getLogger().println("run slackstepsend, step " + step.token+":" + step.botUser+", desc " + slackDesc.getToken()+":"+slackDesc.getBotUser());
            String baseUrl = step.baseUrl != null ? step.baseUrl : slackDesc.getBaseUrl();
            String team = step.teamDomain != null ? step.teamDomain : slackDesc.getTeamDomain();
            String tokenCredentialId = step.tokenCredentialId != null ? step.tokenCredentialId : slackDesc.getTokenCredentialId();
            String token;
            boolean botUser;
            if (step.token != null) {
                token = step.token;
                botUser = step.botUser;
            } else {
                token = slackDesc.getToken();
                botUser = slackDesc.getBotUser();
            }
            String channel = step.channel != null ? step.channel : slackDesc.getRoom();
            String color = step.color != null ? step.color : "";
            String threadTs = step.threadTs != null ? step.threadTs : "";
            boolean replyBroadcast = step.replyBroadcast;

            //placing in console log to simplify testing of retrieving values from global config or from step field; also used for tests
            listener.getLogger().println(Messages.SlackSendStepConfig(step.baseUrl == null, step.teamDomain == null, step.token == null, step.channel == null, step.color == null));

            SlackService slackService = getSlackService(baseUrl, team, token, tokenCredentialId, botUser, channel);
            SlackResponse result = slackService.publish(step.message, color, threadTs, replyBroadcast);
            if (!result.isSuccess() && step.failOnError) {
                throw new AbortException(Messages.NotificationFailed());
            } else if (!result.isSuccess()) {
                listener.error(Messages.NotificationFailed());
            }
            
            // The original implementation of this step returned a boolean value,
            // to support threads we need to return data from the slack sending 
            // to allow the original message to return it's ts value which subsequent 
            // calls can use as the threadTs value.
            // 
            // Since groovy treats an empty map as "falsy", the lets utilise that to
            // keep backwards compatibility.   This means any existing code that 
            // does `def success = slackSend "my message" .. if (success) { .... }`
            // should still work. See http://groovy-lang.org/semantics.html#Groovy-Truth
            Map<String, String> returnValue = new HashMap<String, String>();
            
            // Only populate the response if it was a success, we want an empty map on fail
            // so that it will be coerced to false in groovy.
            if (result.isSuccess()) {
                returnValue.put(SlackResponse.SUCCESS_KEY, Boolean.toString(result.isSuccess()));
                returnValue.put(SlackResponse.CHANNEL_KEY, result.getChannel());
                returnValue.put(SlackResponse.THREAD_TS_KEY, result.getThreadTs());
                returnValue.put(SlackResponse.TS_KEY, result.getTs());
            }
            
            listener.getLogger().println(Messages.SlackSendStepResult(result.isSuccess(), result.getChannel(),
                    result.getTs(), result.getThreadTs()));

            return returnValue;
        }

        //streamline unit testing
        SlackService getSlackService(String baseUrl, String team, String token, String tokenCredentialId, boolean botUser, String channel) {
            return new StandardSlackService(baseUrl, team, token, tokenCredentialId, botUser, channel);
        }
    }
}
