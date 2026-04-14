package com.toolcnc.api.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Global Response Advice để mã hóa dữ liệu API trên môi trường Production.
 * Giúp che giấu cấu trúc dữ liệu JSON trong tab Network của trình duyệt.
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class DataMaskingResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Áp dụng cho tất cả các loại phản hồi
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // Tránh lỗi khi body null hoặc đã là chuỗi Base64 (trường hợp hiếm)
        if (body == null || body instanceof String) {
            return body;
        }

        // Kiểm tra xem có đang ở môi trường local hay không
        boolean isLocal = Arrays.asList(environment.getActiveProfiles()).contains("local");

        // Nếu KHÔNG PHẢI local (là production), thực hiện mã hóa Base64
        if (!isLocal) {
            try {
                // Chuyển đối tượng sang chuỗi JSON
                String jsonBody = objectMapper.writeValueAsString(body);
                
                // Mã hóa chuỗi JSON sang Base64
                return Base64.getEncoder().encodeToString(jsonBody.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Lỗi khi mã hóa phản hồi API: ", e);
                return body; // Trả về body gốc nếu có lỗi để ứng dụng không chết
            }
        }

        // Nếu là môi trường local, trả về body gốc (JSON thuần)
        return body;
    }
}
