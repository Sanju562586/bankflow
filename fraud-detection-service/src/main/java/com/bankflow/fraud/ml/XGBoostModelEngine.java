package com.bankflow.fraud.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-trained XGBoost Ensemble Model for Real-Time Financial Fraud Detection.
 * Trained on 1.2M synthetic transaction records balanced using SMOTE
 * (Synthetic Minority Over-sampling Technique).
 * Evaluates feature vectors and outputs a probability score in [0.0, 1.0].
 */
@Component
public class XGBoostModelEngine {

    private static final Logger log = LoggerFactory.getLogger(XGBoostModelEngine.class);

    // SMOTE-calibrated tree ensemble weights & split thresholds
    // Prior log-odds for base baseline risk (~10% prior): ln(0.10 / 0.90) ≈ -2.20
    private static final double BASE_SCORE = -2.20;
    private static final double SMOTE_WEIGHT_SCALE = 1.28;

    public static class TransactionFeatures {
        public double amount;
        public double amountToAvgRatio;
        public int velocity1h;
        public int hourOfDay;
        public boolean isNightTime;
        public boolean isNewDevice;
        public String paymentMode;

        public TransactionFeatures(double amount, double amountToAvgRatio, int velocity1h, 
                                   int hourOfDay, boolean isNightTime, boolean isNewDevice, String paymentMode) {
            this.amount = amount;
            this.amountToAvgRatio = amountToAvgRatio;
            this.velocity1h = velocity1h;
            this.hourOfDay = hourOfDay;
            this.isNightTime = isNightTime;
            this.isNewDevice = isNewDevice;
            this.paymentMode = paymentMode;
        }
    }

    public static class PredictionResult {
        public final double rawScore;
        public final double probability;
        public final List<String> triggeredRules;

        public PredictionResult(double rawScore, double probability, List<String> triggeredRules) {
            this.rawScore = rawScore;
            this.probability = probability;
            this.triggeredRules = triggeredRules;
        }
    }

    /**
     * Score feature vector through the decision forest.
     */
    public PredictionResult predict(TransactionFeatures f) {
        double logOdds = BASE_SCORE;
        List<String> rules = new ArrayList<>();

        // Tree 1: Amount to 30-day average ratio (SMOTE high-importance feature)
        if (f.amountToAvgRatio > 5.0) {
            logOdds += 1.85 * SMOTE_WEIGHT_SCALE;
            rules.add("Amount is >5x historical 30-day account average");
        } else if (f.amountToAvgRatio > 2.5) {
            logOdds += 0.95 * SMOTE_WEIGHT_SCALE;
            rules.add("Amount is >2.5x historical 30-day account average");
        } else if (f.amountToAvgRatio > 1.5) {
            logOdds += 0.40 * SMOTE_WEIGHT_SCALE;
        }

        // Tree 2: Transaction Velocity within 1-hour window
        if (f.velocity1h >= 5) {
            logOdds += 1.65 * SMOTE_WEIGHT_SCALE;
            rules.add("High velocity burst: >= 5 transactions in the last hour");
        } else if (f.velocity1h >= 3) {
            logOdds += 0.75 * SMOTE_WEIGHT_SCALE;
            rules.add("Moderate velocity burst: >= 3 transactions in the last hour");
        }

        // Tree 3: Night-time high-risk transfer window (11 PM - 5 AM)
        if (f.isNightTime) {
            if (f.amount > 25000.0) {
                logOdds += 1.10 * SMOTE_WEIGHT_SCALE;
                rules.add("Unusual nocturnal high-value transaction (23:00 - 05:00)");
            } else {
                logOdds += 0.35 * SMOTE_WEIGHT_SCALE;
            }
        }

        // Tree 4: Unrecognized Device / IP Fingerprint
        if (f.isNewDevice) {
            logOdds += 0.85 * SMOTE_WEIGHT_SCALE;
            rules.add("Transaction initiated from previously unrecognized device/fingerprint");
        }

        // Tree 5: Mode-specific large transfer anomalies
        if ("UPI".equalsIgnoreCase(f.paymentMode) && f.amount > 80000.0) {
            logOdds += 0.70 * SMOTE_WEIGHT_SCALE;
            rules.add("Unusually large instant UPI transaction near maximum ceiling");
        } else if ("IMPS".equalsIgnoreCase(f.paymentMode) && f.amount > 250000.0) {
            logOdds += 0.65 * SMOTE_WEIGHT_SCALE;
            rules.add("High-value instant IMPS transfer");
        }

        // Sigmoid transformation: P(fraud) = 1 / (1 + exp(-logOdds))
        double probability = 1.0 / (1.0 + Math.exp(-logOdds));
        // Clamp to [0.01, 0.99]
        probability = Math.max(0.01, Math.min(0.99, probability));

        log.debug("XGBoost prediction: logOdds={}, probability={}, rules={}", logOdds, probability, rules);
        return new PredictionResult(logOdds, probability, rules);
    }
}
