package com.medilink.model.medicine;

/**
 * Abstract Decorator class for dynamically enhancing medicine visual metadata.
 * Demonstrates the Decorator Design Pattern.
 */
public abstract class MedicineBadgeDecorator extends Medicine {
    protected Medicine decoratedMedicine;

    public MedicineBadgeDecorator(Medicine decoratedMedicine) {
        super(
            decoratedMedicine.getId(),
            decoratedMedicine.getBrandName(),
            decoratedMedicine.getGenericName(),
            decoratedMedicine.getCompany(),
            decoratedMedicine.getStrength(),
            decoratedMedicine.getFormulation(),
            decoratedMedicine.getUnitPrice(),
            decoratedMedicine.isPrescriptionRequired(),
            decoratedMedicine.getCategory(),
            decoratedMedicine.getSideEffects()
        );
        this.decoratedMedicine = decoratedMedicine;
    }

    @Override
    public String getDisplayBadge() {
        return decoratedMedicine.getDisplayBadge();
    }
}
