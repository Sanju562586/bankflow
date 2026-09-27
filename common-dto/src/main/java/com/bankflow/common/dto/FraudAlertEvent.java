package com.bankflow.common.dto;

import com.bankflow.common.enums.RiskLevel;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class FraudAlertEvent implements Serializable {
    private String alertId;
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private double riskScore; // 0.0 - 1.0 (from XGBoost)
    private RiskLevel riskLevel;
    private boolean flaggedByLlm;
    private String llmExplanation; // Natural-language explanation from Groq LLM
    private List<String> triggeredRules;
    private Instant evaluatedAt;

    public FraudAlertEvent() {}

    public FraudAlertEvent(String alertId, String transactionId, String fromAccountId, String toAccountId,
                           BigDecimal amount, double riskScore, RiskLevel riskLevel, boolean flaggedByLlm,
                           String llmExplanation, List<String> triggeredRules, Instant evaluatedAt) {
        this.alertId = alertId;
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.flaggedByLlm = flaggedByLlm;
        this.llmExplanation = llmExplanation;
        this.triggeredRules = triggeredRules;
        this.evaluatedAt = evaluatedAt;
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(String fromAccountId) { this.fromAccountId = fromAccountId; }

    public String getToAccountId() { return toAccountId; }
    public void setToAccountId(String toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public boolean isFlaggedByLlm() { return flaggedByLlm; }
    public void setFlaggedByLlm(boolean flaggedByLlm) { this.flaggedByLlm = flaggedByLlm; }

    public String getLlmExplanation() { return llmExplanation; }
    public void setLlmExplanation(String llmExplanation) { this.llmExplanation = llmExplanation; }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }

    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
}
