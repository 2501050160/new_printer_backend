package com.saipraveen.login_registration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;

@Service
public class PrinterConfigService {

    @Autowired
    private PrinterConfigRepository repository;

    @Transactional
    public PrinterConfig savePrinter(PrinterConfig printer) {
        PrinterConfig target = null;
        if (printer.getId() != null) {
            target = repository.findById(printer.getId()).orElse(null);
        }

        if (target == null) {
            target = new PrinterConfig();
        }

        target.setBlockLocation(printer.getBlockLocation());
        target.setPrinterName(printer.getPrinterName() != null && !printer.getPrinterName().trim().isEmpty() 
            ? printer.getPrinterName().trim() 
            : (printer.getBlockLocation() != null ? printer.getBlockLocation() + " Printer" : "Default Printer"));
        target.setPrinterIp(printer.getPrinterIp() != null && !printer.getPrinterIp().trim().isEmpty() 
            ? printer.getPrinterIp().trim() 
            : "192.168.1.100");
        target.setMaintenance(printer.getMaintenance() != null ? printer.getMaintenance() : false);
        target.setQrScanToPrint(printer.getQrScanToPrint() != null ? printer.getQrScanToPrint() : false);
        target.setOtpEnabled(printer.getOtpEnabled() != null ? printer.getOtpEnabled() : true);
        target.setColourSupported(printer.getColourSupported() != null ? printer.getColourSupported() : false);
        target.setPaused(printer.getPaused() != null ? printer.getPaused() : false);
        target.setBwDisabledForColor(printer.getBwDisabledForColor() != null ? printer.getBwDisabledForColor() : false);
        target.setPaperCount(printer.getPaperCount() != null ? printer.getPaperCount() : 500);

        boolean isActive = printer.getActive() != null ? printer.getActive() : false;
        target.setActive(isActive);

        target = repository.save(target);

        // If making this printer active, ensure only 1 active printer for this color type in this block
        if (isActive && target.getBlockLocation() != null) {
            boolean isColor = target.getColourSupported();
            List<PrinterConfig> sameTypePrinters = repository.findByBlockLocationAndColourSupported(target.getBlockLocation(), isColor);
            for (PrinterConfig p : sameTypePrinters) {
                if (!p.getId().equals(target.getId())) {
                    p.setActive(false);
                    repository.save(p);
                }
            }
        }

        return target;
    }

    public PrinterConfig toggleBwForColor(Long id) {
        PrinterConfig printer = repository.findById(id).orElse(null);
        if (printer != null) {
            boolean current = printer.getBwDisabledForColor();
            printer.setBwDisabledForColor(!current);
            return repository.save(printer);
        }
        return null;
    }

    public List<PrinterConfig> getAllPrinters() {
        return repository.findAll();
    }

    public List<PrinterConfig> getAllPrintersByBlock(String blockLocation) {
        return repository.findAllByBlockLocation(blockLocation);
    }

    public PrinterConfig getPrinterByBlock(String blockLocation) {
        PrinterConfig active = repository.findFirstByBlockLocationAndActiveTrue(blockLocation);
        if (active != null) {
            return active;
        }
        return repository.findByBlockLocation(blockLocation);
    }

    public PrinterConfig getPrinterByBlockAndType(String blockLocation, String printType) {
        boolean isColor = "COLOR".equalsIgnoreCase(printType);
        PrinterConfig activeTypePrinter = repository.findFirstByBlockLocationAndColourSupportedAndActiveTrue(blockLocation, isColor);
        if (activeTypePrinter != null) {
            // If job is BW, but the active color printer has BW disabled, check if there is an active BW printer
            if (!isColor && Boolean.TRUE.equals(activeTypePrinter.getBwDisabledForColor())) {
                PrinterConfig activeBwPrinter = repository.findFirstByBlockLocationAndColourSupportedAndActiveTrue(blockLocation, false);
                if (activeBwPrinter != null) {
                    return activeBwPrinter;
                }
            } else {
                return activeTypePrinter;
            }
        }
        // Fallback to any active printer in the block that supports the requested printType
        PrinterConfig fallbackActive = repository.findFirstByBlockLocationAndActiveTrue(blockLocation);
        if (fallbackActive != null) {
            return fallbackActive;
        }
        return repository.findByBlockLocation(blockLocation);
    }

    public void deletePrinter(Long id) {
        repository.deleteById(id);
    }

    public void decrementPaper(String blockLocation, int pages) {
        PrinterConfig printer = getPrinterByBlock(blockLocation);
        if (printer != null) {
            int current = printer.getPaperCount() != null ? printer.getPaperCount() : 0;
            int newCount = Math.max(0, current - pages);
            printer.setPaperCount(newCount);
            repository.save(printer);
        }
    }

    public void updatePaperCount(String blockLocation, int count) {
        List<PrinterConfig> printers = repository.findAllByBlockLocation(blockLocation);
        if (printers != null && !printers.isEmpty()) {
            for (PrinterConfig printer : printers) {
                printer.setPaperCount(count);
                repository.save(printer);
            }
        } else {
            PrinterConfig printer = repository.findByBlockLocation(blockLocation);
            if (printer != null) {
                printer.setPaperCount(count);
                repository.save(printer);
            }
        }
    }

    public static class AvailabilityResult {
        private final boolean available;
        private final String message;

        public AvailabilityResult(boolean available, String message) {
            this.available = available;
            this.message = message;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getMessage() {
            return message;
        }
    }

    public AvailabilityResult checkPrinterAvailability(String blockLocation, String printType) {
        if (blockLocation == null || blockLocation.trim().isEmpty()) {
            return new AvailabilityResult(true, "OK");
        }

        String block = blockLocation.trim();
        List<PrinterConfig> printers = repository.findAllByBlockLocation(block);
        if (printers == null || printers.isEmpty()) {
            // Check if any printer exists at all in the database
            if (repository.count() == 0) {
                return new AvailabilityResult(true, "OK");
            }
            return new AvailabilityResult(false, "No printer is currently configured for kiosk block '" + block + "'. Please select another block.");
        }

        boolean isColor = "COLOR".equalsIgnoreCase(printType);

        List<PrinterConfig> activePrinters = printers.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .toList();

        if (activePrinters.isEmpty()) {
            return new AvailabilityResult(false, "The printer at '" + block + "' is currently offline or inactive. Please select another block.");
        }

        boolean allMaintenanceOrPaused = activePrinters.stream()
                .allMatch(p -> Boolean.TRUE.equals(p.getMaintenance()) || Boolean.TRUE.equals(p.getPaused()));
        if (allMaintenanceOrPaused) {
            boolean hasMaint = activePrinters.stream().anyMatch(p -> Boolean.TRUE.equals(p.getMaintenance()));
            if (hasMaint) {
                return new AvailabilityResult(false, "The printer at '" + block + "' is currently under maintenance. Please select another block.");
            } else {
                return new AvailabilityResult(false, "The printer at '" + block + "' is currently paused. Please select another block.");
            }
        }

        if (isColor) {
            boolean hasColor = activePrinters.stream()
                    .anyMatch(p -> Boolean.TRUE.equals(p.getColourSupported()) && !Boolean.TRUE.equals(p.getMaintenance()) && !Boolean.TRUE.equals(p.getPaused()));
            if (!hasColor) {
                return new AvailabilityResult(false, "Color printing is not available at '" + block + "'. Please choose Black & White or select a Color-enabled kiosk.");
            }
        } else {
            boolean hasBw = activePrinters.stream()
                    .anyMatch(p -> (!Boolean.TRUE.equals(p.getColourSupported()) || !Boolean.TRUE.equals(p.getBwDisabledForColor()))
                            && !Boolean.TRUE.equals(p.getMaintenance()) && !Boolean.TRUE.equals(p.getPaused()));
            if (!hasBw) {
                return new AvailabilityResult(false, "Black & White printing is temporarily disabled at '" + block + "'.");
            }
        }

        boolean hasPaper = activePrinters.stream().anyMatch(p -> p.getPaperCount() == null || p.getPaperCount() > 0);
        if (!hasPaper) {
            return new AvailabilityResult(false, "The printer at '" + block + "' is currently out of paper. Please select another kiosk block.");
        }

        return new AvailabilityResult(true, "OK");
    }
}