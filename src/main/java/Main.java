import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.logging.Logging;

import javax.swing.table.DefaultTableModel;

public class Main implements BurpExtension, HttpHandler {

    MontoyaApi api;
    Logging logging;

    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("Proxy Match Auto Highlight");
        this.logging = api.logging();
        this.logging.logToOutput("Extension loaded!");

        api.http().registerHttpHandler(this);
        api.userInterface().registerSuiteTab("ProxyMatchAutoHighlight", new MainUI());
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        String stringPath = httpRequestToBeSent.path();
        DefaultTableModel model3 = (DefaultTableModel) MainUI.HighlighterTable1.getModel();
        for(int i = 0; i < model3.getRowCount(); i++) {
            String endPoint = (String) model3.getValueAt(i, 0);
            if (stringPath.contains(endPoint)){
                return RequestToBeSentAction.continueWith(httpRequestToBeSent, httpRequestToBeSent.annotations().withHighlightColor(HighlightColor.YELLOW));
            }
        }
        return RequestToBeSentAction.continueWith(httpRequestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        return null;
    }
}
