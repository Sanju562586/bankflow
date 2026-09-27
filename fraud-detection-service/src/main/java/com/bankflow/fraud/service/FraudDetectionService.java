package com.bankflow.fraud.service;

import com.bankflow.common.dto.FraudAlertEvent;
import com.bankflow.common.dto.TransactionApprovedEvent;
import com.bankflow.common.dto.TransactionInitiatedEvent;
import com.bankflow.common.enums.RiskLevel;
import com.bankflow.fraud.dto.FraudEvaluationResult;
import com.bankflow.fraud.event.FraudEventProducer;
import com.bankflow.fraud.ml.XGBoostModelEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final XGBoostModelEngine modelEngine;
    private final GroqLlmService groqLlmService;
    private final AccountVelocityTracker velocityTracker;
    private final FraudEventProducer eventProducer;

    // In-memory cache of recent alerts and evaluations for API and UI
    private final Deque<FraudAlertEvent> recentAlerts = new ConcurrentLinkedDeque<>();
    private final Deque<FraudEvaluationResult> recentEvaluations = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY = 100;

    // Thresholds
    private static final double LOW_RISK_THRESHOLD = 0.35;
    private static final double HIGH_RISK_THRESHOLD = 0.75;

    public FraudDetectionService(XGBoostModelEngine modelEngine, 
                                 GroqLlmService groqLlmService, 
                                 AccountVelocityTracker velocityTracker, 
                                 FraudEventProducer eventProducer) {
        this.modelEngine = modelEngine;
        this.groqLlmService = groqLlmService;
        this.velocityTracker = velocityTracker;
        this.eventProducer = eventProducer;
    }

    public FraudEvaluationResult evaluate(TransactionInitiatedEvent event) {
        String txnId = event.getTransactionId();
        double amount = event.getAmount().doubleValue();
        String fromAccount = event.getFromAccountId();

        // 1. Gather features
        double historicalAvg = velocityTracker.getHistorical30DayAverage(fromAccount);
        double ratio = historicalAvg > 0 ? (amount / historicalAvg) : 1.0;
        int velocity1h = velocityTracker.getVelocityLastHour(fromAccount);

        Instant initiatedAt = event.getInitiatedAt() != null ? event.getInitiatedAt() : Instant.now();
        int hourOfDay = ZonedDateTime.ofInstant(initiatedAt, ZoneId.of("Asia/Kolkata")).getHour();
        boolean isNight = (hourOfDay >= 23 || hourOfDay <= 5);
        boolean isNewDevice = (event.getDeviceFingerprint() != null && event.getDeviceFingerprint().hashCode() % 3 == 0);
        String paymentMode = event.getPaymentMode() != null ? event.getPaymentMode().name() : "UPI";

        XGBoostModelEngine.TransactionFeatures features = new XGBoostModelEngine.TransactionFeatures(
            amount, ratio, velocity1h, hourOfDay, isNight, isNewDevice, paymentMode
        );

        // 2. Score with XGBoost model
        XGBoostModelEngine.PredictionResult prediction = modelEngine.predict(features);
        double score = prediction.probability;
        List<String> rules = new ArrayList<>(prediction.triggeredRules);

        boolean ambiguous = (score >= LOW_RISK_THRESHOLD && score <= HIGH_RISK_THRESHOLD);
        boolean isFraud = (score > HIGH_RISK_THRESHOLD);
        boolean llmConsulted = false;
        String llmExplanation = null;

        // 3. If ambiguous, trigger Groq LLM reasoning
        if (ambiguous) {
            llmConsulted = true;
            GroqLlmService.LlmDecision llmDecision = groqLlmService.evaluateAmbiguousTransaction(
                txnId, amount, historicalAvg, ratio, velocity1h, hourOfDay, isNewDevice, paymentMode, score
            );
            isFraud = llmDecision.isFraud;
            llmExplanation = llmDecision.rationale;
            rules.add("Groq LLM evaluated: " + (isFraud ? "REJECTED" : "APPROVED"));
        }

        // Determine RiskLevel
        RiskLevel riskLevel;
        if (score >= 0.80) riskLevel = RiskLevel.CRITICAL;
        else if (score >= 0.60) riskLevel = RiskLevel.HIGH;
        else if (score >= 0.35) riskLevel = RiskLevel.MEDIUM;
        else riskLevel = RiskLevel.LOW;

        FraudEvaluationResult result = new FraudEvaluationResult(
            txnId, score, riskLevel, isFraud, ambiguous, llmConsulted, llmExplanation, rules
        );

        // Record velocity for sender
        velocityTracker.recordTransaction(fromAccount, initiatedAt);

        // Cache evaluation result
        recentEvaluations.addFirst(result);
        if (recentEvaluations.size() > MAX_HISTORY) recentEvaluations.removeLast();

        // 4. Publish resulting Kafka event
        if (isFraud) {
            FraudAlertEvent alertEvent = new FraudAlertEvent(
                "alert-" + UUID.randomUUID().toString().substring(0, 8),
                txnId,
                fromAccount,
                event.getToAccountId(),
                event.getAmount(),
                score,
                riskLevel,
                llmConsulted,
                llmExplanation,
                rules,
                Instant.now()
            );
            recentAlerts.addFirst(alertEvent);
            if (recentAlerts.size() > MAX_HISTORY) recentAlerts.removeLast();
            eventProducer.publishFraudAlert(alertEvent);
        } else {
            TransactionApprovedEvent approvedEvent = new TransactionApprovedEvent(
                txnId,
                event.getIdempotencyKey(),
                fromAccount,
                event.getToAccountId(),
                event.getAmount(),
                score,
                llmExplanation != null ? llmExplanation : "Low risk assessment passed",
                Instant.now()
            );
            eventProducer.publishTransactionApproved(approvedEvent);
        }

        return result;
    }

    public List<FraudAlertEvent> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }

    public List<FraudEvaluationResult> getRecentEvaluations() {
        return new ArrayList<>(recentEvaluations);
    }
}
