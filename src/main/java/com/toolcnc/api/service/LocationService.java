package com.toolcnc.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolcnc.api.dto.ProvinceResponse;
import com.toolcnc.api.dto.WardResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private List<ProvinceResponse> provinces = new ArrayList<>();
    private Map<Integer, List<WardResponse>> provinceToWardsMap = new HashMap<>();

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Path relative to the project root where the JSON files are located
            String path = "database-province-ward/commune-ward.json";
            File file = new File(path);
            
            if (!file.exists()) {
                System.err.println("Location data file not found at: " + file.getAbsolutePath());
                return;
            }

            JsonNode root = mapper.readTree(file);
            if (root.isArray()) {
                for (JsonNode provinceNode : root) {
                    int pCode = provinceNode.get("code").asInt();
                    String pName = provinceNode.get("name").asText();
                    
                    provinces.add(new ProvinceResponse(pCode, pName));
                    
                    List<WardResponse> wards = new ArrayList<>();
                    JsonNode wardsNode = provinceNode.get("wards");
                    if (wardsNode != null && wardsNode.isArray()) {
                        for (JsonNode wardNode : wardsNode) {
                            int wCode = wardNode.get("code").asInt();
                            String wName = wardNode.get("name").asText();
                            wards.add(new WardResponse(wCode, wName));
                        }
                    }
                    provinceToWardsMap.put(pCode, wards);
                }
            }
            System.out.println("Location data loaded successfully: " + provinces.size() + " provinces.");
        } catch (IOException e) {
            System.err.println("Error loading location data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<ProvinceResponse> getProvinces() {
        return provinces;
    }

    public List<WardResponse> getWards(Integer provinceCode) {
        return provinceToWardsMap.getOrDefault(provinceCode, Collections.emptyList());
    }
}
