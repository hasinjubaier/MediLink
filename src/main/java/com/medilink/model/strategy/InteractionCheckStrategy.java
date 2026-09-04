package com.medilink.model.strategy;

import java.util.List;

/**
 * Strategy interface for checking drug-to-drug interactions.
 */
public interface InteractionCheckStrategy {
    String evaluateRisk(List<String> genericNames);
    String getStrategyLevel();
}
