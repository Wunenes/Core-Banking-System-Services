package com.UserService.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class TokenObtainService {
    protected String obtainTokenFromAuthServer() {

        String tokenUrl = "http://localhost:9000/oauth2/token";
        RestTemplate restTemplate = new RestTemplate();

        // Set up the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("client-id", "client-secret");

        // Set up the request body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "account:write account:read account:transaction");
        body.add("include_client_id", "true");

        // Create the HTTP entity with headers and body
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        // Make the request
        ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                requestEntity,
                Map.class
        );

        // Extract the access token
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to obtain access token: " + response.getStatusCode());
        }
    }
}
