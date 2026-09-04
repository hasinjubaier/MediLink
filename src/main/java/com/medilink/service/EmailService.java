package com.medilink.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

/**
 * EmailService — sends transactional emails via Gmail SMTP.
 *
 * ┌─────────────────────────────────────────────────────────┐
 *  SETUP (one-time):
 *  1. Put your Gmail address in SENDER_EMAIL below.
 *  2. Generate a Gmail App Password:
 *     Google Account → Security → 2-Step Verification → App Passwords
 *     (App: "Mail", Device: "Other → MediLink")
 *  3. Paste the 16-char app password in SENDER_APP_PASSWORD.
 * └─────────────────────────────────────────────────────────┘
 */
public class EmailService {

    // ── CONFIGURE THESE TWO LINES ──────────────────────────
    private static final String SENDER_EMAIL       = "your_gmail@gmail.com";   // <-- your Gmail
    private static final String SENDER_APP_PASSWORD = "xxxx xxxx xxxx xxxx";   // <-- App Password
    // ───────────────────────────────────────────────────────

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    private static EmailService instance;

    private EmailService() {}

    public static synchronized EmailService getInstance() {
        if (instance == null) instance = new EmailService();
        return instance;
    }

    /**
     * Sends a 6-digit OTP email to the given recipient.
     *
     * @param toEmail  Recipient email address
     * @param otp      The 6-digit OTP code
     * @throws MessagingException if the email cannot be sent
     */
    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL + " (MediLink)"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("MediLink — Your Verification Code");
        message.setContent(buildHtmlBody(otp), "text/html; charset=UTF-8");

        Transport.send(message);
        System.out.println("[EmailService] OTP email sent to: " + toEmail);
    }

    /** Checks whether email credentials have been configured. */
    public boolean isConfigured() {
        return !SENDER_EMAIL.startsWith("your_gmail")
            && !SENDER_APP_PASSWORD.startsWith("xxxx");
    }

    // ── HTML email template ────────────────────────────────
    private String buildHtmlBody(String otp) {
        return "<!DOCTYPE html>" +
        "<html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;" +
        "background:#f4f9fb;font-family:Inter,Arial,sans-serif;'>" +
        "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px;'>" +
        "<table width='520' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:16px;" +
        "box-shadow:0 8px 32px rgba(27,122,140,0.10);overflow:hidden;'>" +

        // Header
        "<tr><td style='background:linear-gradient(135deg,#1b7a8c 0%,#0d9488 100%);" +
        "padding:32px 40px 28px;text-align:center;'>" +
        "<div style='display:inline-flex;align-items:center;gap:10px;'>" +
        "<span style='font-size:28px;'>🛡️</span>" +
        "<span style='color:#ffffff;font-size:22px;font-weight:800;letter-spacing:-0.5px;'>MediLink</span>" +
        "</div>" +
        "<p style='color:rgba(255,255,255,0.85);font-size:14px;margin:10px 0 0;'>Secure Email Verification</p>" +
        "</td></tr>" +

        // Body
        "<tr><td style='padding:36px 40px 28px;'>" +
        "<h2 style='color:#0c2331;font-size:20px;font-weight:700;margin:0 0 12px;'>Verify Your Email Address</h2>" +
        "<p style='color:#4a6170;font-size:15px;line-height:1.6;margin:0 0 28px;'>" +
        "Use the one-time verification code below to complete your MediLink registration. " +
        "This code is valid for <strong>5 minutes</strong>." +
        "</p>" +

        // OTP box
        "<div style='background:#f0f9fb;border:2px dashed #1b7a8c;border-radius:12px;" +
        "padding:24px;text-align:center;margin:0 0 28px;'>" +
        "<p style='color:#7892a0;font-size:13px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;margin:0 0 10px;'>Your OTP Code</p>" +
        "<span style='font-size:44px;font-weight:900;letter-spacing:14px;color:#1b7a8c;" +
        "font-family:\"JetBrains Mono\",\"Courier New\",monospace;'>" + otp + "</span>" +
        "</div>" +

        "<p style='color:#7892a0;font-size:13px;line-height:1.5;margin:0;'>" +
        "⚠️ Do not share this code with anyone. MediLink will never ask for your OTP via phone or chat." +
        "</p>" +
        "</td></tr>" +

        // Footer
        "<tr><td style='background:#f4f9fb;padding:20px 40px;text-align:center;border-top:1px solid #e2edf1;'>" +
        "<p style='color:#7892a0;font-size:12px;margin:0;'>" +
        "© 2026 MediLink — Connecting Healthcare. If you didn't request this, ignore this email." +
        "</p>" +
        "</td></tr>" +

        "</table></td></tr></table></body></html>";
    }
}
