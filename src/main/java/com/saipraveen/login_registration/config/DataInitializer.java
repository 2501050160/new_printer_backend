package com.saipraveen.login_registration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.saipraveen.login_registration.entity.Admin;
import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.Pricing;
import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.entity.SystemSetting;
import com.saipraveen.login_registration.repository.AdminRepository;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.PricingRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;
import com.saipraveen.login_registration.repository.SystemSettingRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CampusBlockRepository blockRepository;

    @Autowired
    private PricingRepository pricingRepository;

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. Initialize default Main Admin account if empty
            if (adminRepository.count() == 0 || adminRepository.findByUsername("admin") == null) {
                Admin admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword("admin123");
                admin.setRole("MAIN_ADMIN");
                admin.setCollege("ALL");
                adminRepository.save(admin);
            }

            // 2. Initialize default Campus Blocks if empty
            if (blockRepository.count() == 0) {
                blockRepository.save(new CampusBlock("Central Library", "KLU"));
                blockRepository.save(new CampusBlock("CSE Block (Ground Floor)", "KLU"));
                blockRepository.save(new CampusBlock("Mechanical Block", "KLU"));
            }

            // 3. Initialize default Pricing if empty
            if (pricingRepository.count() == 0) {
                Pricing bw1 = new Pricing();
                bw1.setPrintType("Black & White");
                bw1.setPricePerPage(2.0);
                bw1.setBlockLocation("Central Library");
                pricingRepository.save(bw1);

                Pricing col1 = new Pricing();
                col1.setPrintType("Color");
                col1.setPricePerPage(5.0);
                col1.setBlockLocation("Central Library");
                pricingRepository.save(col1);

                Pricing bw2 = new Pricing();
                bw2.setPrintType("Black & White");
                bw2.setPricePerPage(2.0);
                bw2.setBlockLocation("CSE Block (Ground Floor)");
                pricingRepository.save(bw2);

                Pricing col2 = new Pricing();
                col2.setPrintType("Color");
                col2.setPricePerPage(5.0);
                col2.setBlockLocation("CSE Block (Ground Floor)");
                pricingRepository.save(col2);
            }

            // 4. Initialize default System Settings if empty
            String[][] settings = {
                {"referralEnabled", "true"},
                {"referrerAmount", "10.0"},
                {"refereeAmount", "5.0"},
                {"popupEnabled", "true"},
                {"popupMessage", "🎉 Refer a friend to earn 10.0 free print credits!"},
                {"adEnabled", "true"},
                {"adText", "Print thesis/assignments directly from your phone and skip lines!"},
                {"generalPopupEnabled", "false"},
                {"generalPopupMessage", ""},
                {"razorpayChargePercentage", "2.36"},
                {"managerMaxColorPrinters", "1"},
                {"managerMaxBwPrinters", "1"}
            };
            for (String[] pair : settings) {
                if (!settingRepository.existsById(pair[0])) {
                    settingRepository.save(new SystemSetting(pair[0], pair[1]));
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: DataInitializer startup check deferred: " + e.getMessage());
        }
    }
}
