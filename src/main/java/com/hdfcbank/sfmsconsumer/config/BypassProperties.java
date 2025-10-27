package com.hdfcbank.sfmsconsumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "bypass")
public class BypassProperties {

    private boolean enabled;
    private String defaultSwitch;
    private Map<String, String> switches = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultSwitch() {
        return defaultSwitch;
    }

    public void setDefaultSwitch(String defaultSwitch) {
        this.defaultSwitch = defaultSwitch;
    }

    public Map<String, String> getSwitches() {
        return switches;
    }

    public void setSwitches(Map<String, String> switches) {
        this.switches = switches;
    }

    public String getTopicForSwitch(String sw) {
        if (sw == null) return null;
        return switches.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(sw))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
