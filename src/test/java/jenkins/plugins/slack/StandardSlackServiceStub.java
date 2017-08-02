package jenkins.plugins.slack;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;

public class StandardSlackServiceStub extends StandardSlackService {

    private CloseableHttpClientStub httpClientStub;
    private String responseString;

    public StandardSlackServiceStub(String baseUrl, String teamDomain, String token, String tokenCredentialId, boolean botUser, String roomId) {
        super(baseUrl, teamDomain, token, tokenCredentialId, botUser, roomId);
    }

    @Override
    public CloseableHttpClientStub getHttpClient() {
        return httpClientStub;
    }

    public void setHttpClient(CloseableHttpClientStub httpClientStub) {
        this.httpClientStub = httpClientStub;
    }
    
    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    @Override
    String getEntityAsString(CloseableHttpResponse httpResponse) throws ParseException, IOException {
        return responseString;
    }
}
