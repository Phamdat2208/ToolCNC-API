package com.toolcnc.api.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Global Response Advice để mã hóa dữ liệu API trên môi trường Production.
 * Giúp che giấu cấu trúc dữ liệu JSON trong tab Network của trình duyệt.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class DataMaskingResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Value("${api.masking.enabled:true}")
    private boolean maskingEnabled;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Áp dụng cho tất cả các loại phản hồi JSON hoặc Object
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // Tránh lỗi khi body null hoặc đã là chuỗi Base64
        if (body == null || body instanceof String) {
            return body;
        }

        // Thực hiện mã hóa Base64 nếu tính năng này được bật
        if (maskingEnabled) {
            try {
                // Chuyển đối tượng sang chuỗi JSON
                String jsonBody = objectMapper.writeValueAsString(body);
                
                // Mã hóa chuỗi JSON sang Base64
                String encodedData = Base64.getEncoder().encodeToString(jsonBody.getBytes(StandardCharsets.UTF_8));
                
                // Ghi log để kiểm tra (chỉ dùng khi debug)
                // log.debug("API Response masked successfully");
                
                return encodedData;
            } catch (Exception e) {
                log.error("Lỗi khi mã hóa phản hồi API: ", e);
                return body; // Trả về body gốc nếu có lỗi để ứng dụng không chết
            }
        }

        // Nếu tính năng bị tắt (Local), trả về body gốc (JSON thuần)
        return body;
    }
}
