package com.acmecorp.orders;

public class OrderProcessor {

    // Referenced twice below -- should NOT be flagged as orphan.
    void checkNewCheckout(FeatureFlags flags) {
        if (flags.isEnabled("new-checkout-flow")) {
            applyNewCheckout();
        }
    }

    void applyNewCheckout(FeatureFlags flags) {
        // Second reference to the same flag key, elsewhere in the project.
        if (flags.isEnabled("new-checkout-flow")) {
            System.out.println("new checkout applied");
        }
    }

    // Only 1 reference total (this call itself) -- rollout finished, should be flagged as orphan candidate.
    void checkLegacyDiscount(FeatureFlags flags) {
        if (flags.isEnabled("legacy-discount-banner-2024")) {
            System.out.println("legacy path");
        }
    }
}

interface FeatureFlags {
    boolean isEnabled(String key);
}
