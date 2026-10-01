package com.drrcp.reporting;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditProbeService {

    public String register(Map<String, String> details) {
        return "registered";
    }
}