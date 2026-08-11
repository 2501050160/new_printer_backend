package com.saipraveen.login_registration.service;

import com.saipraveen.login_registration.entity.CampusBlock;
import com.saipraveen.login_registration.entity.PrinterConfig;
import com.saipraveen.login_registration.repository.CampusBlockRepository;
import com.saipraveen.login_registration.repository.PrinterConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampusBlockService {

    @Autowired
    private CampusBlockRepository blockRepository;

    @Autowired
    private PrinterConfigRepository printerConfigRepository;

    public List<CampusBlock> getAllBlocks() {
        return blockRepository.findAll();
    }

    public CampusBlock createBlock(String name, String college) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be empty");
        }
        String trimmed = name.trim();
        if (blockRepository.findByName(trimmed) != null) {
            throw new IllegalArgumentException("Block already exists");
        }
        String col = (college == null || college.trim().isEmpty()) ? "KLU" : college.trim();
        return blockRepository.save(new CampusBlock(trimmed, col));
    }

    @Transactional
    public CampusBlock renameBlock(Long id, String newName) {
        CampusBlock block = blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Block not found"));
        String oldName = block.getName();
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("New name cannot be empty");
        }
        String trimmed = newName.trim();
        CampusBlock conflict = blockRepository.findByName(trimmed);
        if (conflict != null && !conflict.getId().equals(id)) {
            throw new IllegalArgumentException("Another block already uses this name");
        }
        block.setName(trimmed);
        CampusBlock saved = blockRepository.save(block);
        // Update printers referencing the old block location
        List<PrinterConfig> printers = printerConfigRepository.findAll();
        for (PrinterConfig pc : printers) {
            if (oldName.equals(pc.getBlockLocation())) {
                pc.setBlockLocation(trimmed);
                printerConfigRepository.save(pc);
            }
        }
        return saved;
    }

    @Transactional
    public void deleteBlock(Long id) {
        CampusBlock block = blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Block not found"));
        String name = block.getName();
        // Deactivate printers belonging to this block
        List<PrinterConfig> printers = printerConfigRepository.findAll();
        for (PrinterConfig pc : printers) {
            if (name.equals(pc.getBlockLocation())) {
                pc.setActive(false);
                printerConfigRepository.save(pc);
            }
        }
        blockRepository.delete(block);
    }
}
