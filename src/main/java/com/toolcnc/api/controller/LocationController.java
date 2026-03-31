package com.toolcnc.api.controller;

import com.toolcnc.api.dto.ProvinceResponse;
import com.toolcnc.api.dto.WardResponse;
import com.toolcnc.api.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@CrossOrigin(origins = "*") // Adjust to your front-end URL if needed
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/provinces")
    public List<ProvinceResponse> getProvinces() {
        return locationService.getProvinces();
    }

    @GetMapping("/wards")
    public List<WardResponse> getWards(@RequestParam Integer provinceCode) {
        return locationService.getWards(provinceCode);
    }
}
