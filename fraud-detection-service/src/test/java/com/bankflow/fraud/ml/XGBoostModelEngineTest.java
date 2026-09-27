package com.bankflow.fraud.ml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XGBoostModelEngineTest {

    private final XGBoostModelEngine engine = new XGBoostModelEngine();

    @Test
    @DisplayName("predict: normal transaction scores low probability and zero rules")
    void testNormalTransactionLowRisk() {
        XGBoostModelEngine.TransactionFeatures features = new XGBoostModelEngine.TransactionFeatures(
                1200.0, 1.0, 1, 14, false, false, "UPI"
        );

        XGBoostModelEngine.PredictionResult result = engine.predict(features);

        assertThat(result.probability).isLessThan(0.35);
        assertThat(result.triggeredRules).isEmpty();
    }

    @Test
    @DisplayName("predict: high amount-to-average ratio triggers >5x rule")
    void testHighRatioTriggersRule() {
        XGBoostModelEngine.TransactionFeatures features = new XGBoostModelEngine.TransactionFeatures(
                90000.0, 6.0, 1, 14, false, false, "UPI"
        );

        XGBoostModelEngine.PredictionResult result = engine.predict(features);

        assertThat(result.probability).isGreaterThan(0.60);
        assertThat(result.triggeredRules).anyMatch(r -> r.contains(">5x historical"));
    }

    @Test
    @DisplayName("predict: nocturnal transfer with new device and velocity burst scores critical")
    void testCriticalRiskCombination() {
        XGBoostModelEngine.TransactionFeatures features = new XGBoostModelEngine.TransactionFeatures(
                85000.0, 5.5, 6, 2, true, true, "UPI"
        );

        XGBoostModelEngine.PredictionResult result = engine.predict(features);

        assertThat(result.probability).isGreaterThan(0.80);
        assertThat(result.triggeredRules.size()).isGreaterThanOrEqualTo(3);
    }
}
