package com.medilink.model.medicine;

/**
 * Concrete Decorator adding Low Stock Warning badge.
 */
public class LowStockBadgeDecorator extends MedicineBadgeDecorator {
    private int currentUnits;

    public LowStockBadgeDecorator(Medicine decoratedMedicine, int currentUnits) {
        super(decoratedMedicine);
        this.currentUnits = currentUnits;
    }

    public int getCurrentUnits() {
        return currentUnits;
    }

    @Override
    public String getDisplayBadge() {
        return decoratedMedicine.getDisplayBadge() + " | LOW_STOCK_CRITICAL[" + currentUnits + " units left]";
    }
}
