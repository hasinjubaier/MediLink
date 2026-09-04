package com.medilink.model.medicine;

/**
 * Concrete Decorator adding Verified Authentic badge.
 */
public class VerifiedBadgeDecorator extends MedicineBadgeDecorator {
    private String dgdaVerificationCode;

    public VerifiedBadgeDecorator(Medicine decoratedMedicine, String dgdaVerificationCode) {
        super(decoratedMedicine);
        this.dgdaVerificationCode = dgdaVerificationCode;
    }

    public String getDgdaVerificationCode() {
        return dgdaVerificationCode;
    }

    @Override
    public String getDisplayBadge() {
        return decoratedMedicine.getDisplayBadge() + " | VERIFIED_GENUINE_DGDA[" + dgdaVerificationCode + "]";
    }
}
