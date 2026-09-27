package com.bankflow.fraud.dto;

import com.bankflow.common.enums.RiskLevel;

import java.util.List;

public class FraudEvaluationResult {
    private String transactionId;
    private double riskScore;
    private RiskLevel riskLevel;
    private boolean isFraud;
    private boolean ambiguousScore;
    private boolean llmConsulted;
    private String llmExplanation;
    private List<String> triggeredRules;

    public FraudEvaluationResult() {}

    public FraudEvaluationResult(String transactionId, double riskScore, RiskLevel riskLevel, 
                                 boolean isFraud, boolean ambiguousScore, boolean llmConsulted, 
                                 String llmExplanation, List<String> triggeredRules) {
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.isFraud = isFraud;
        this.ambiguousScore = ambiguousScore;
        this.llmConsulted = llmConsulted;
        this.llmExplanation = llmExplanation;
        this.triggeredRules = triggeredRules;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public boolean isFraud() { return isFraud; }
    public void setFraud(boolean fraud) { isFraud = fraud; }

    public boolean isAmbiguousScore() { return ambiguousScore; }
    public void setAmbiguousScore(boolean ambiguousScore) { this.ambiguousScore = ambiguousScore; }

    public boolean isLlmConsulted() { return llmConsulted; }
    public void setLlmConsulted(boolean llmConsulted) { this.llmConsulted = llmConsulted; }

    public String getLlmExplanation() { return llmExplanation; }
    public void setLlmExplanation(String llmExplanation) { this.llmExplanation = llmExplanation; }

    public List<String> getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(List<String> triggeredRules) { this.triggeredRules = triggeredRules; }
}
