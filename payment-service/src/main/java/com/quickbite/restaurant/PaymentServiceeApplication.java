package com.quickbite.restaurant;

/**
 * DEPRECATED — This class should NOT be used.
 * The actual entry point is {@link com.quickbite.payment.PaymentServiceApplication}.
 *
 * This file was originally created here by mistake (wrong package).
 * If your IDE is running this class instead of PaymentServiceApplication,
 * please change your run configuration to use com.quickbite.payment.PaymentServiceApplication.
 */
public class PaymentServiceeApplication {
    public static void main(String[] args) {
        // Delegate to the real application class
        com.quickbite.payment.PaymentServiceApplication.main(args);
    }
}
