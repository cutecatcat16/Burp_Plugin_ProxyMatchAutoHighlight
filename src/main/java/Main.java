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
        api.userInterface().registerSuiteTab("ProxyMatchAutoHighlight", new newMainUI());
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        if (httpRequestToBeSent.isInScope()) {

            String stringPath = httpRequestToBeSent.path();
            String stringPathNoQueries = stringPath.contains("?") ? stringPath.substring(0, stringPath.indexOf("?")) : stringPath;
            DefaultTableModel modelFromAPIMapperTable = (DefaultTableModel) newMainUI.APIMapperTable.getModel();
            DefaultTableModel modelFromProxyMatchTable = (DefaultTableModel) newMainUI.ProxyMatchTable.getModel();
            modelFromAPIMapperTable.addRow(new Object[]{stringPathNoQueries});
            for (int i = 0; i < modelFromProxyMatchTable.getRowCount(); i++) {
                String endPoint = (String) modelFromProxyMatchTable.getValueAt(i, 0);
                if (stringPath.contains(endPoint)) {
                    return RequestToBeSentAction.continueWith(httpRequestToBeSent, httpRequestToBeSent.annotations().withHighlightColor(HighlightColor.YELLOW));
                }
            }
            return RequestToBeSentAction.continueWith(httpRequestToBeSent);
        }
        return null;
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        return null;
    }
}
