package com.saipraveen.login_registration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.saipraveen.login_registration.entity.SystemSetting;
import com.saipraveen.login_registration.repository.SystemSettingRepository;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository repository;

    public String getSetting(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getKeyValue)
                .orElse(defaultValue);
    }

    /**
     * Upsert a setting: update the value if the key exists, insert if it doesn't.
     * Using findById + setKeyValue avoids duplicate key violations on String @Id entities.
     */
    public void setSetting(String key, String value) {
        SystemSetting setting = repository.findById(key)
                .orElse(new SystemSetting(key, value));
        setting.setKeyValue(value);
        repository.save(setting);
    }

    public boolean getSettingBool(String key, boolean defaultValue) {
        String val = getSetting(key, null);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public double getSettingDouble(String key, double defaultValue) {
        String val = getSetting(key, null);
        try {
            return val != null ? Double.parseDouble(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getSettingInt(String key, int defaultValue) {
        String val = getSetting(key, null);
        try {
            return val != null ? Integer.parseInt(val) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public java.util.Map<String, Object> getSettings() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("referralEnabled", getSettingBool("referral_enabled", true));
        map.put("referrerAmount", getSettingDouble("referral_referrer_amount", 10.0));
        map.put("refereeAmount", getSettingDouble("referral_referee_amount", 5.0));
        map.put("popupEnabled", getSettingBool("referral_popup_enabled", true));
        map.put("popupMessage", getSetting("referral_popup_message", ""));
        map.put("adEnabled", getSettingBool("ad_enabled", true));
        map.put("adText", getSetting("ad_text", ""));
        map.put("testerModeEnabled", getSettingBool("tester_mode_enabled", false));
        map.put("testerUsernames", getSetting("tester_usernames", ""));
        return map;
    }
}
