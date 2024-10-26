package tauon.app.util.misc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.markusbernhardt.proxy.ProxySearch;

import java.net.ProxySelector;
import java.net.URL;

import static tauon.app.util.misc.Constants.API_UPDATE_URL;

public class VersionEntry implements Comparable<VersionEntry> {
    
    private String tagName;
    
    public VersionEntry() {
        // Constructor for Version checker
    }
    public VersionEntry(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public int compareTo(VersionEntry o) {
        int v1 = getNumericValue();
        int v2 = o.getNumericValue();
        return v1 - v2;
    }

    public final int getNumericValue() {
        String[] arr = tagName.substring(1).split("\\.");
        int value = 0;
        int multiplier = 1;
        for (int i = arr.length - 1; i >= 0; i--) {
            value += Integer.parseInt(arr[i]) * multiplier;
            multiplier *= 10;
        }
        return value;
    }
    
    @JsonProperty("tag_name")
    public String getTagName() {
        return tagName;
    }
    
    @JsonProperty("tag_name")
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public String toString() {
        return "VersionEntry [tag_name=" + tagName + " value=" + getNumericValue() + "]";
    }
    
    
    public static VersionEntry getLastVersionFromGithub() {
        try {
            ProxySearch proxySearch = ProxySearch.getDefaultProxySearch();
            ProxySelector myProxySelector = proxySearch.getProxySelector();
            ProxySelector.setDefault(myProxySelector);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return objectMapper.readValue(new URL(API_UPDATE_URL).openStream(),
                    new TypeReference<VersionEntry>() {
                    });
        } catch (Exception e) {
            return null;
        }
    }
    
}
