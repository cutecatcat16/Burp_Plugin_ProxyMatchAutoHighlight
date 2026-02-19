import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.logging.Logging;

public class Main implements BurpExtension, HttpHandler {

    MontoyaApi api;
    Logging logging;

    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Proxy Match Auto Highlight");
        this.logging = api.logging();
        this.logging.logToOutput("Extension loaded!");

        newMainUI ui = new newMainUI();
        newMainUI.initializePersistence(api.persistence().extensionData());
        api.userInterface().registerSuiteTab("ProxyMatchAutoHighlight", ui);
        api.http().registerHttpHandler(this);
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        if (!httpRequestToBeSent.isInScope()) {
            return RequestToBeSentAction.continueWith(httpRequestToBeSent);
        }

        String path = httpRequestToBeSent.path();
        String pathNoQueries = newMainUI.stripQueryAndFragment(path);
        newMainUI.addMappedEndpoint(pathNoQueries);

        HighlightColor matchedColor = newMainUI.findMatchingColor(pathNoQueries);
        if (matchedColor != null && matchedColor != HighlightColor.NONE) {
            return RequestToBeSentAction.continueWith(
                    httpRequestToBeSent,
                    httpRequestToBeSent.annotations().withHighlightColor(matchedColor)
            );
        }
        return RequestToBeSentAction.continueWith(httpRequestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        return ResponseReceivedAction.continueWith(httpResponseReceived);
    }
}
