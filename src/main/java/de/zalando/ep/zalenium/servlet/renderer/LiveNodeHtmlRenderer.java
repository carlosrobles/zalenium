package de.zalando.ep.zalenium.servlet.renderer;

import com.google.gson.JsonObject;
import de.zalando.ep.zalenium.proxy.DockerSeleniumRemoteProxy;
import org.openqa.grid.internal.TestSession;
import org.openqa.grid.internal.TestSlot;
import org.openqa.grid.internal.utils.HtmlRenderer;
import org.openqa.grid.web.servlet.beta.MiniCapability;
import org.openqa.grid.web.servlet.beta.SlotsLines;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.CapabilityType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LiveNodeHtmlRenderer implements HtmlRenderer {

    private static final Logger LOGGER = Logger.getLogger(LiveNodeHtmlRenderer.class.getName());

    private DockerSeleniumRemoteProxy proxy;
    private String serverName;
    private TemplateRenderer templateRenderer;

    @SuppressWarnings("WeakerAccess")
    public LiveNodeHtmlRenderer(DockerSeleniumRemoteProxy proxy, String serverName) {
        this.proxy = proxy;
        this.serverName = serverName;
        this.templateRenderer = new TemplateRenderer(getTemplateName());
    }

    /**
     * Platform for docker-selenium will be always Linux.
     *
     * @param proxy remote proxy
     * @return Either the platform name, "Unknown", "mixed OS", or "not specified".
     */
    @SuppressWarnings("WeakerAccess")
    public static String getPlatform(DockerSeleniumRemoteProxy proxy) {
        return getPlatform(proxy.getTestSlots().get(0)).toString();
    }

    private static Platform getPlatform(TestSlot slot) {
        return (Platform) slot.getCapabilities().get(CapabilityType.PLATFORM);
    }

    private String getTemplateName() {
        return "html_templates/live_node_tab.html";
    }

    @Override
    public String renderSummary() {
        StringBuilder testName = new StringBuilder();
        if (!proxy.getTestName().isEmpty()) {
            testName.append("<p>Test name: ").append(proxy.getTestName()).append("</p>");
        }
        StringBuilder testGroup = new StringBuilder();
        if (!proxy.getTestGroup().isEmpty()) {
            testGroup.append("<p>Test group: ").append(proxy.getTestGroup()).append("</p>");
        }

        SlotsLines wdLines = new SlotsLines();
        TestSlot testSlot = proxy.getTestSlots().get(0);
        wdLines.add(testSlot);
        MiniCapability miniCapability = wdLines.getLinesType().iterator().next();
        String icon = miniCapability.getIcon();
        String version = miniCapability.getVersion();
        TestSession session = testSlot.getSession();
        String slotClass = "";
        String slotTitle;
        if (session != null) {
            slotClass = "busy";
            slotTitle = session.get("lastCommand") == null ? "" : session.get("lastCommand").toString();
        } else {
            slotTitle = testSlot.getCapabilities().toString();
        }

        // Adding live preview
        int noVncPort = proxy.getRegistration().getNoVncPort();
        String noVncViewBaseUrl = "http://%s:%s/?view_only=%s";
        String noVncReadOnlyUrl = String.format(noVncViewBaseUrl, serverName, noVncPort, "true");
        String noVncInteractUrl = String.format(noVncViewBaseUrl, serverName, noVncPort, "false");

        Map<String, String> renderSummaryValues = new HashMap<>();
        renderSummaryValues.put("{{proxyName}}", proxy.getClass().getSimpleName());
        renderSummaryValues.put("{{proxyVersion}}", getHtmlNodeVersion());
        renderSummaryValues.put("{{proxyId}}", proxy.getId());
        renderSummaryValues.put("{{proxyPlatform}}", getPlatform(proxy));
        renderSummaryValues.put("{{testName}}", testName.toString());
        renderSummaryValues.put("{{testGroup}}", testGroup.toString());
        renderSummaryValues.put("{{browserVersion}}", version);
        renderSummaryValues.put("{{slotIcon}}", icon);
        renderSummaryValues.put("{{slotClass}}", slotClass);
        renderSummaryValues.put("{{slotTitle}}", slotTitle);
        renderSummaryValues.put("{{noVncReadOnlyUrl}}", noVncReadOnlyUrl);
        renderSummaryValues.put("{{noVncInteractUrl}}", noVncInteractUrl);
        renderSummaryValues.put("{{tabConfig}}", proxy.getConfig().toString("<p>%1$s: %2$s</p>"));
        return templateRenderer.renderTemplate(renderSummaryValues);
    }

    private String getHtmlNodeVersion() {
        try {
            JsonObject object = proxy.getStatus();
            String version = object.get("value").getAsJsonObject()
                    .get("build").getAsJsonObject()
                    .get("version").getAsString();
            return " (version : " + version + ")";
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.toString(), e);
            return " unknown version, " + e.getMessage();
        }
    }
}
