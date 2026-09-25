package com.medilink.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Central configuration manager for AI Vision Services (Google Gemini + Groq).
 * Securely loads credentials from environment variables, system properties,
 * root .env file, or medilink_config.properties. Never logs raw keys.
 */
@Component
public class AiVisionConfig {

    private static final String DEFAULT_GEMINI_MODEL = "gemini-1.5-flash";
    private static final String DEFAULT_GROQ_MODEL = "llama-3.2-11b-vision-preview";

    @Value("${gemini.api.key:}")
    private String configuredGeminiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String configuredGeminiModel;

    @Value("${groq.api.key:}")
    private String configuredGroqKey;

    @Value("${groq.model:llama-3.2-11b-vision-preview}")
    private String configuredGroqModel;

    private String resolvedGeminiKey = "";
    private String resolvedGeminiModel = DEFAULT_GEMINI_MODEL;
    private String resolvedGroqKey = "";
    private String resolvedGroqModel = DEFAULT_GROQ_MODEL;
    private long lastEnvModified = -1;
    private File activeEnvFile = null;

    @PostConstruct
    public void init() {
        reloadConfig();
    }

    public synchronized void reloadConfig() {
        Map<String, String> dotEnv = loadDotEnv();

        // 1. Resolve Gemini API Key: System env -> Property -> .env -> config file -> @Value
        String gKey = System.getenv("GEMINI_API_KEY");
        if (isEmpty(gKey)) gKey = System.getProperty("gemini.api.key");
        if (isEmpty(gKey)) gKey = dotEnv.get("GEMINI_API_KEY");
        if (isEmpty(gKey)) gKey = loadFromConfigFile("gemini.api.key");
        if (isEmpty(gKey)) gKey = configuredGeminiKey;
        this.resolvedGeminiKey = clean(gKey);

        // 2. Resolve Gemini Model
        String gModel = System.getenv("GEMINI_MODEL");
        if (isEmpty(gModel)) gModel = System.getProperty("gemini.model");
        if (isEmpty(gModel)) gModel = dotEnv.get("GEMINI_MODEL");
        if (isEmpty(gModel)) gModel = configuredGeminiModel;
        this.resolvedGeminiModel = !isEmpty(gModel) ? gModel.trim() : DEFAULT_GEMINI_MODEL;

        // 3. Resolve Groq API Key: System env -> Property -> .env -> config file -> @Value
        String qKey = System.getenv("GROQ_API_KEY");
        if (isEmpty(qKey)) qKey = System.getProperty("groq.api.key");
        if (isEmpty(qKey)) qKey = dotEnv.get("GROQ_API_KEY");
        if (isEmpty(qKey)) qKey = loadFromConfigFile("groq.api.key");
        if (isEmpty(qKey)) qKey = configuredGroqKey;
        this.resolvedGroqKey = clean(qKey);

        // 4. Resolve Groq Model
        String qModel = System.getenv("GROQ_MODEL");
        if (isEmpty(qModel)) qModel = System.getProperty("groq.model");
        if (isEmpty(qModel)) qModel = dotEnv.get("GROQ_MODEL");
        if (isEmpty(qModel)) qModel = configuredGroqModel;
        this.resolvedGroqModel = !isEmpty(qModel) ? qModel.trim() : DEFAULT_GROQ_MODEL;

        // Safe operational audit logging (never expose key characters)
        System.out.println("[AI Vision Config] Initialized/Reloaded (source: " + (activeEnvFile != null ? activeEnvFile.getAbsolutePath() : "environment/properties") + "):");
        System.out.println("  - Gemini API: " + (isGeminiConfiguredInternal() ? "Configured (" + maskKey(resolvedGeminiKey) + "), Model=" + resolvedGeminiModel : "NOT CONFIGURED"));
        System.out.println("  - Groq API:   " + (isGroqConfiguredInternal() ? "Configured (" + maskKey(resolvedGroqKey) + "), Model=" + resolvedGroqModel : "NOT CONFIGURED"));
    }

    private void ensureFreshConfig() {
        if (!isGeminiConfiguredInternal() || !isGroqConfiguredInternal() || isEnvModified()) {
            reloadConfig();
        }
    }

    private boolean isEnvModified() {
        if (activeEnvFile != null && activeEnvFile.exists()) {
            return activeEnvFile.lastModified() != lastEnvModified;
        }
        // If no env file was found previously, check if one was created
        File f = findEnvFile();
        return f != null && f.exists();
    }

    private boolean isGeminiConfiguredInternal() {
        return resolvedGeminiKey != null && resolvedGeminiKey.length() > 5;
    }

    private boolean isGroqConfiguredInternal() {
        return resolvedGroqKey != null && resolvedGroqKey.length() > 5;
    }

    public boolean isGeminiConfigured() {
        ensureFreshConfig();
        return isGeminiConfiguredInternal();
    }

    public boolean isGroqConfigured() {
        ensureFreshConfig();
        return isGroqConfiguredInternal();
    }

    public String getGeminiApiKey() {
        ensureFreshConfig();
        return resolvedGeminiKey;
    }

    public void setGeminiApiKey(String key) {
        this.resolvedGeminiKey = clean(key);
    }

    public String getGeminiModel() {
        ensureFreshConfig();
        return resolvedGeminiModel;
    }

    public void setGeminiModel(String model) {
        if (!isEmpty(model)) this.resolvedGeminiModel = model.trim();
    }

    public String getGroqApiKey() {
        ensureFreshConfig();
        return resolvedGroqKey;
    }

    public void setGroqApiKey(String key) {
        this.resolvedGroqKey = clean(key);
    }

    public String getGroqModel() {
        ensureFreshConfig();
        return resolvedGroqModel;
    }

    public void setGroqModel(String model) {
        if (!isEmpty(model)) this.resolvedGroqModel = model.trim();
    }

    public String maskKey(String key) {
        if (key == null || key.trim().isEmpty()) return "NONE";
        String t = key.trim();
        if (t.length() <= 8) return "****";
        return t.substring(0, 4) + "..." + t.substring(t.length() - 4);
    }

    private File findEnvFile() {
        String userDir = System.getProperty("user.dir", ".");
        String[] possiblePaths = {
            ".env",
            "../.env",
            "../../.env",
            userDir + File.separator + ".env",
            userDir + File.separator + ".." + File.separator + ".env",
            "medilink/.env",
            "d:/ACADEMIC CAREER/12th Semester/Advance OOP/Medilink2.0/.env"
        };
        for (String p : possiblePaths) {
            try {
                File f = new File(p);
                if (f.exists() && f.isFile() && f.canRead()) {
                    return f;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        File f = findEnvFile();
        if (f != null && f.exists() && f.isFile()) {
            this.activeEnvFile = f;
            this.lastEnvModified = f.lastModified();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("\uFEFF")) {
                        trimmed = trimmed.substring(1).trim();
                    }
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        String k = trimmed.substring(0, eqIdx).trim();
                        String v = trimmed.substring(eqIdx + 1).trim();
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                            v = v.substring(1, v.length() - 1);
                        }
                        if (!k.isEmpty()) {
                            map.put(k, v);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    private String loadFromConfigFile(String key) {
        String[] possiblePaths = {
            "database/medilink_config.properties",
            "medilink_config.properties",
            "../database/medilink_config.properties"
        };
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    Properties p = new Properties();
                    p.load(in);
                    String val = p.getProperty(key);
                    if (val != null && !val.trim().isEmpty()) {
                        return val.trim();
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String clean(String str) {
        return str != null ? str.trim() : "";
    }
}
