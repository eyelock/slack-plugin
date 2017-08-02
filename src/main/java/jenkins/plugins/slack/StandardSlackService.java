package jenkins.plugins.slack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.ProxyConfiguration;
import hudson.security.ACL;
import jenkins.model.Jenkins;

public class StandardSlackService implements SlackService {

    private static final Logger logger = Logger.getLogger(StandardSlackService.class.getName());

    private String host = "slack.com";
    private String baseUrl;
    private String teamDomain;
    private String token;
    private String authTokenCredentialId;
    private boolean botUser;
    private String[] roomIds;

    public StandardSlackService(String baseUrl, String teamDomain, String token, String authTokenCredentialId, boolean botUser, String roomId) {
        super();
        this.baseUrl = baseUrl;
        if(this.baseUrl != null && !this.baseUrl.isEmpty() && !this.baseUrl.endsWith("/")) {
            this.baseUrl += "/";
        }
        this.teamDomain = teamDomain;
        this.token = token;
        this.authTokenCredentialId = StringUtils.trim(authTokenCredentialId);
        this.botUser = botUser;
        this.roomIds = roomId.split("[,; ]+");
    }

    @Override
    public SlackResponse publish(String message) {
        return publish(message, "warning");
    }

    @Override
    public SlackResponse publish(String message, String color) {
        return publish(message, color, "", false);
    }

    @Override
    public SlackResponse publish(String message, String color, String threadTs, boolean replyBroadcast) {
        SlackResponse result = new SlackResponse();
        result.setSuccess(true);
        boolean isThreaded = StringUtils.isNotEmpty(threadTs);

        for (String roomId : roomIds) {
            //prepare attachments first
            JSONObject attachment = new JSONObject();
            attachment.put("text", message);
            attachment.put("fallback", message);
            attachment.put("color", color);

            if (isThreaded) {
                // If we are sending to a thread, we can't go across multiple channels, you cannot
                // send a threaded message to more than one channel. Slack will return an OK, but
                // the message does not get shown in Slack.
                if (roomIds.length > 1) {
                    result.setSuccess(false);
                    logger.log(Level.SEVERE,
                            String.format(
                                    "Error posting to Slack: Cannot send threaded message to more than 1 room, no messages will be sent.  Found trying to send to: %s",
                                    roomIds.toString()));
                    break;
                }

                result.setThreadTs(threadTs);
                attachment.put("thread_ts", threadTs);
                attachment.put("reply_broadcast", replyBroadcast);
            }

            JSONArray mrkdwn = new JSONArray();
            mrkdwn.put("pretext");
            mrkdwn.put("text");
            mrkdwn.put("fields");
            attachment.put("mrkdwn_in", mrkdwn);
            JSONArray attachments = new JSONArray();
            attachments.put(attachment);

            HttpPost post;
            String url;
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            //prepare post methods for both requests types
            if (!botUser || !StringUtils.isEmpty(baseUrl)) {
                url = "https://" + teamDomain + "." + host + "/services/hooks/jenkins-ci?token=" + getTokenToUse();
                if (!StringUtils.isEmpty(baseUrl)) {
                    url = baseUrl + getTokenToUse();
                }
                post = new HttpPost(url);
                JSONObject json = new JSONObject();

                json.put("channel", roomId);
                json.put("attachments", attachments);
                json.put("link_names", "1");

                nvps.add(new BasicNameValuePair("payload", json.toString()));
            } else {
                url = "https://slack.com/api/chat.postMessage?token=" + getTokenToUse() +
                        "&channel=" + roomId +
                        "&link_names=1" +
                        "&as_user=true";
                try {
                    url += "&attachments=" + URLEncoder.encode(attachments.toString(), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    logger.log(Level.ALL, "Error while encoding attachments: " + e.getMessage());
                }
                post = new HttpPost(url);
            }
            logger.fine("Posting: to " + roomId + " on " + teamDomain + " using " + url + ": " + message + " " + color
                    + "(threadTs = " + threadTs + " with replyBroadcast = " + replyBroadcast + ")");
            CloseableHttpClient client = getHttpClient();

            try {
            	post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            	CloseableHttpResponse response = client.execute(post);
            	
            	int responseCode = response.getStatusLine().getStatusCode();
            	if(responseCode != HttpStatus.SC_OK) {
                    logger.log(Level.WARNING, "Slack post may have failed. Response: " + getEntityAsString(response));
                    logger.log(Level.WARNING, "Response Code: " + responseCode);
                    result.setSuccess(false);
                } else {
                    populateResponse(response, result);

                    // Slack will return an OK when you post a threaded message to a channel
                    // that was not the original channel, the key difference in a success v
                    // a fail in this scenario is the existence of the thread_ts value, meaning
                    // a successful posting that found the thread. We should treat this as a
                    // failure.
                    if (isThreaded && result.getThreadTs() == null) {
                        logger.log(Level.WARNING, "Slack threaded post has failed to find the thread in channel "
                                + roomId + " with thread_ts " + threadTs
                                + ".  Please check the original message channel and ts response.");
                        result.setSuccess(false);
                        continue;
                    } else {
                        logger.info("Posting succeeded. Response: " + getEntityAsString(response));
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error posting to Slack", e);
                result.setSuccess(false);
            } finally {
                post.releaseConnection();
            }
        }
        return result;
    }

    private String getTokenToUse() {
        if (authTokenCredentialId != null && !authTokenCredentialId.isEmpty()) {
            StringCredentials credentials = lookupCredentials(authTokenCredentialId);
            if (credentials != null) {
                logger.fine("Using Integration Token Credential ID.");
                return credentials.getSecret().getPlainText();
            }
        }

        logger.fine("Using Integration Token.");

        return token;
    }

    private StringCredentials lookupCredentials(String credentialId) {
        List<StringCredentials> credentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialId);
        return CredentialsMatchers.firstOrNull(credentials, matcher);
    }

    protected CloseableHttpClient getHttpClient() {
    	final HttpClientBuilder clientBuilder = HttpClients.custom();
    	final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    	clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    	
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                final HttpHost proxyHost = new HttpHost(proxy.name, proxy.port);
                final HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
                clientBuilder.setRoutePlanner(routePlanner);                

                String username = proxy.getUserName();
                String password = proxy.getPassword();
                // Consider it to be passed if username specified. Sufficient?
                if (username != null && !"".equals(username.trim())) {
                    logger.info("Using proxy authentication (user=" + username + ")");
                    credentialsProvider.setCredentials(new AuthScope(proxyHost), 
                    								   new UsernamePasswordCredentials(username, password));
                }
            }
        }
        return clientBuilder.build();
    }

    void setHost(String host) {
        this.host = host;
    }

    void populateResponse(CloseableHttpResponse httpResponse, SlackResponse slackResponse)
            throws IOException {
        String slackString = getEntityAsString(httpResponse);
        JSONObject slackJson = new JSONObject(slackString);
        
        String channel = slackJson.getString("channel");
        String threadTs = slackJson.getJSONObject("message").getString("thread_ts");
        String ts = slackJson.getString("ts");

        slackResponse.setSuccess(slackJson.getBoolean("ok"));
        slackResponse.setChannel(StringUtils.isNotEmpty(channel) ? channel : null);
        slackResponse.setThreadTs(StringUtils.isNotEmpty(threadTs) ? threadTs : null);
        slackResponse.setTs(StringUtils.isNotEmpty(ts) ? ts : null);
    }

    String getEntityAsString(CloseableHttpResponse httpResponse) throws IOException {
        return EntityUtils.toString(httpResponse.getEntity());
    }
}
