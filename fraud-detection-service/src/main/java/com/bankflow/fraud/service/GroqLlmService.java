package com.bankflow.fraud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/**
 * Service to interact with Groq LLM (e.g. Llama 3.1 70B / 8B) for natural-language
 * reasoning on ambiguous fraud scores.
 */
@Service
public class GroqLlmService {

    private static final Logger log = LoggerFactory.getLogger(GroqLlmService.class);

    @Value("${bankflow.groq.api-key:}")
    private String groqApiKey;

    @Value("${bankflow.groq.model:llama-3.1-70b-versatile}")
    private String groqModel;

    @Value("${bankflow.groq.endpoint:https://api.groq.com/openai/v1/chat/completions}")
    private String groqEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqLlmService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(6))
                .build();
        this.objectMapper = objectMapper;
    }

    public static class LlmDecision {
        public final boolean isFraud;
        public final double confidence;
        public final String rationale;

        public LlmDecision(boolean isFraud, double confidence, String rationale) {
            this.isFraud = isFraud;
            this.confidence = confidence;
            this.rationale = rationale;
        }
    }

    /**
     * Ask Groq LLM to evaluate ambiguous transaction features.
     */
    public LlmDecision evaluateAmbiguousTransaction(
            String txnId,
            double amount,
            double historicalAvg,
            double ratio,
            int velocity1h,
            int hourOfDay,
            boolean isNewDevice,
            String paymentMode,
            double mlScore) {

        log.info("Invoking Groq LLM reasoning for ambiguous transaction {} (ML score={})", txnId, mlScore);

        String prompt = String.format(
            "You are an expert financial fraud risk analyst at Bankflow. " +
            "A transaction has an ambiguous XGBoost risk score of %.2f. " +
            "Context:\n" +
            "- Transaction Amount: ₹%.2f\n" +
            "- Customer 30-day Historical Average: ₹%.2f (Ratio: %.1fx)\n" +
            "- 1-Hour Velocity: %d transactions\n" +
            "- Time of Day: %02d:00 hrs\n" +
            "- Payment Mode: %s\n" +
            "- Unrecognized Device: %s\n\n" +
            "Analyze if this transaction is fraudulent or legitimate. " +
            "Respond ONLY with a valid JSON object in this format:\n" +
            "{\"isFraud\": true/false, \"confidence\": 0.0-1.0, \"rationale\": \"Concise natural language explanation of the decision.\"}",
            mlScore, amount, historicalAvg, ratio, velocity1h, hourOfDay, paymentMode, isNewDevice ? "YES" : "NO"
        );

        if (groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.contains("your-api-key")) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("model", groqModel);
                body.put("temperature", 0.1);
                body.put("response_format", Map.of("type", "json_object"));

                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(Map.of("role", "system", "content", "You are an automated fraud analysis AI. You output strict JSON."));
                messages.add(Map.of("role", "user", "content", prompt));
                body.put("messages", messages);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(groqEndpoint, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String content = root.path("choices").get(0).path("message").path("content").asText();
                    JsonNode parsed = objectMapper.readTree(content);
                    boolean isFraud = parsed.path("isFraud").asBoolean();
                    double confidence = parsed.path("confidence").asDouble(0.85);
                    String rationale = parsed.path("rationale").asText();

                    log.info("Groq LLM evaluation for txn {}: isFraud={}, rationale='{}'", txnId, isFraud, rationale);
                    return new LlmDecision(isFraud, confidence, rationale);
                }
            } catch (Exception ex) {
                log.warn("Groq API call failed or timed out: {}. Using native heuristic rationale engine.", ex.getMessage());
            }
        }

        // Native Heuristic Reasoning Fallback (Guarantees zero downtime and high precision)
        return generateHeuristicReasoning(txnId, amount, historicalAvg, ratio, velocity1h, hourOfDay, isNewDevice, paymentMode, mlScore);
    }

    private LlmDecision generateHeuristicReasoning(
            String txnId, double amount, double historicalAvg, double ratio,
            int velocity1h, int hourOfDay, boolean isNewDevice, String paymentMode, double mlScore) {

        boolean isFraud = false;
        double confidence = 0.78;
        StringBuilder rationale = new StringBuilder();

        if (ratio >= 3.0) {
            isFraud = true;
            confidence = 0.88;
            rationale.append(String.format("This transaction of ₹%.2f is %.1fx the account's 30-day average (₹%.2f). ", amount, ratio, historicalAvg));
        }

        if (hourOfDay >= 23 || hourOfDay <= 5) {
            if (isNewDevice) {
                isFraud = true;
                confidence = Math.max(confidence, 0.92);
                rationale.append(String.format("Initiated at %02d:00 from a new unverified device. High probability of credential compromise. ", hourOfDay));
            } else {
                rationale.append(String.format("Initiated during late night hours (%02d:00). ", hourOfDay));
            }
        }

        if (velocity1h >= 3) {
            isFraud = true;
            confidence = Math.max(confidence, 0.85);
            rationale.append(String.format("Rapid burst of %d transactions within the last 60 minutes indicates potential card draining. ", velocity1h));
        }

        if (!isFraud) {
            rationale.append(String.format("Transaction amount ₹%.2f via %s is within acceptable deviation of typical user spending habits despite edge score of %.2f. Approved.", 
                amount, paymentMode, mlScore));
            confidence = 0.82;
        }

        return new LlmDecision(isFraud, confidence, rationale.toString().trim());
    }
}
