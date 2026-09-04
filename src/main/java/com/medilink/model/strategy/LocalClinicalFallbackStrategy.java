package com.medilink.model.strategy;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fallback AI Strategy providing local conversational clinical intelligence.
 * Detects common languages for greetings and provides a language-aware response.
 * For full multilingual generative AI, users should configure a Gemini API key.
 */
public class LocalClinicalFallbackStrategy implements AiChatStrategy {

    @Override
    public String getProviderName() {
        return "CLINICAL_FALLBACK";
    }

    @Override
    public boolean isConfigured(String apiKey) {
        return true;
    }

    @Override
    public String ask(String userMessage, List<Map<String, String>> conversationHistory, String patientContext, String apiKey) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Hello! How can I assist you with your health or medications today?";
        }

        String raw = userMessage.trim();
        String query = raw.toLowerCase(Locale.ROOT);

        // --------------------------------------------------------
        // MULTILINGUAL GREETING DETECTION
        // Detect the script/language and respond in that language.
        // --------------------------------------------------------

        // Bengali / Bangla (Unicode range: \u0980-\u09FF)
        if (containsBengali(raw)) {
            return respondBengali(query, raw);
        }

        // Arabic script (Unicode range: \u0600-\u06FF)
        if (containsArabic(raw)) {
            return respondArabic(query, raw);
        }

        // Hindi / Devanagari script (Unicode range: \u0900-\u097F)
        if (containsDevanagari(raw)) {
            return respondHindi(query, raw);
        }

        // --------------------------------------------------------
        // LATIN-SCRIPT MULTILINGUAL GREETINGS
        // --------------------------------------------------------

        // Spanish / Portuguese
        if (query.equals("hola") || query.startsWith("hola ") || query.equals("buenas") || query.equals("buenos dias") || query.equals("oi") || query.equals("bom dia")) {
            return "Hola! Soy **MediLink**, tu asistente de salud disponible las 24 horas.\n\n" +
                   "Puedes preguntarme sobre medicamentos, dosis, interacciones de farmacos o tus sintomas. Como puedo ayudarte hoy?\n\n" +
                   "_Para respuestas avanzadas con IA generativa, conecta tu clave gratuita de Google Gemini._";
        }

        // French
        if (query.equals("bonjour") || query.equals("salut") || query.equals("bonsoir") || query.startsWith("bonjour ") || query.startsWith("salut ")) {
            return "Bonjour! Je suis **MediLink**, votre assistant de sante disponible 24h/24.\n\n" +
                   "Vous pouvez me poser des questions sur vos medicaments, vos doses, les interactions medicamenteuses ou vos symptomes. Comment puis-je vous aider?\n\n" +
                   "_Pour des reponses generatives avancees, connectez votre cle Google Gemini gratuite._";
        }

        // German
        if (query.equals("hallo") || query.equals("guten tag") || query.equals("guten morgen") || query.equals("guten abend") || query.startsWith("hallo ")) {
            return "Hallo! Ich bin **MediLink**, Ihr rund um die Uhr verfugbarer Gesundheitsassistent.\n\n" +
                   "Sie konnen mich nach Medikamenten, Dosierungen, Wechselwirkungen oder Symptomen fragen. Wie kann ich Ihnen heute helfen?\n\n" +
                   "_Fur erweiterte KI-Antworten schliessen Sie bitte Ihren kostenlosen Google Gemini API-Schlussel an._";
        }

        // Turkish
        if (query.equals("merhaba") || query.equals("selam") || query.startsWith("merhaba ") || query.startsWith("selam ")) {
            return "Merhaba! Ben **MediLink**, 7/24 hizmetinizde olan saglik asistanınızım.\n\n" +
                   "Ilaclar, dozaj, ilaç etkilesimleri veya belirtileriniz hakkinda soru sorabilirsiniz. Bugün size nasil yardimci olabilirim?\n\n" +
                   "_Gelismis uretken yapay zeka yanıtları icin ucretsiz Google Gemini API anahtarinizi baglayin._";
        }

        // Malay / Indonesian
        if (query.equals("hei") || query.equals("hai") || query.equals("selamat pagi") || query.equals("selamat siang") || query.equals("selamat malam") || query.equals("apa khabar") || query.equals("apa kabar")) {
            return "Halo! Saya **MediLink**, asisten kesehatan Anda yang tersedia 24/7.\n\n" +
                   "Anda dapat bertanya tentang obat-obatan, dosis, interaksi obat, atau gejala Anda. Apa yang bisa saya bantu hari ini?\n\n" +
                   "_Untuk respons AI generatif yang lebih lengkap, hubungkan kunci Google Gemini gratis Anda._";
        }

        // --------------------------------------------------------
        // ENGLISH CONVERSATIONAL HANDLING
        // --------------------------------------------------------

        // Natural English Greetings
        if (isGreeting(query)) {
            return "Hello! I am **MediLink**, your 24/7 personal healthcare assistant.\n\n" +
                   "How are you feeling today? You can ask me anything about:\n" +
                   "- **Medications & Dosages** (e.g. Napa, Omeprazole, Seclo)\n" +
                   "- **Symptoms & Discomfort** (e.g. fever, headache, acidity)\n" +
                   "- **Drug Interactions** between your prescriptions\n" +
                   "- **Missed Doses & Proper Timing**\n\n" +
                   "How can I help you right now?";
        }

        // Well-being
        if (query.contains("how are you") || query.contains("how r u") || query.contains("how're you")) {
            return "I'm doing well, thank you for asking! I'm here 24/7 and ready to help you with your health questions.\n\n" +
                   "How are you feeling today? Are you experiencing any symptoms or looking for information about your medicines?";
        }

        // Identity
        if (query.contains("who are you") || query.contains("what are you") || query.contains("what can you do")) {
            return "I am **MediLink**, an intelligent clinical assistant designed for patients on the MediLink 2.0 platform.\n\n" +
                   "I understand multiple languages and can help with medication dosages, drug interactions, side effects, and health guidance in your preferred language.\n\n" +
                   "Feel free to ask any question in your language!";
        }

        // Gratitude
        if (query.contains("thank") || query.contains("thx") || query.contains("appreciate")) {
            return "You're very welcome! Your health and safety are always our top priority.\n\n" +
                   "Feel free to ask any other health or medication questions anytime!";
        }

        // Farewells
        if (query.equals("bye") || query.startsWith("bye ") || query.contains("goodbye") || query.contains("good night") || query.contains("see you")) {
            return "Take care and stay well! Remember to take your prescribed medications on time. I'm here 24/7 whenever you need guidance!";
        }

        // --------------------------------------------------------
        // CLINICAL RESPONSES (English)
        // --------------------------------------------------------

        // Emergency triage
        if (query.contains("chest pain") || query.contains("heart attack") || query.contains("cannot breathe") ||
            query.contains("shortness of breath") || query.contains("unconscious") || query.contains("severe bleeding") ||
            query.contains("choking") || query.contains("stroke")) {
            return "CRITICAL MEDICAL ALERT:\n\n" +
                   "The symptoms you described may indicate an acute medical emergency.\n\n" +
                   "- Please call emergency services immediately (Call 999 or 911) or go to the nearest hospital emergency room.\n" +
                   "- Do not attempt to drive yourself.\n" +
                   "- Rest in a comfortable, upright position and stay calm until medical personnel arrive.";
        }

        // Paracetamol / Napa
        if (query.contains("paracetamol") || query.contains("napa") || query.contains("acetaminophen") || query.contains("fever") || query.contains("headache") || query.contains("body ache")) {
            return "Clinical Guidance: Paracetamol (Napa / Ace)\n\n" +
                   "- **Primary Uses:** Relieving mild to moderate pain (headaches, body aches) and reducing fever.\n" +
                   "- **Standard Adult Dose:** 500mg to 1000mg every 4 to 6 hours as needed (do not exceed 4,000mg in 24 hours).\n" +
                   "- **How to Take:** Swallow with a full glass of water, preferably after food to avoid gastric irritation.\n" +
                   "- **Safety Note:** Avoid combining with other cold or flu medications containing paracetamol to prevent liver toxicity.";
        }

        // Gastric / Omeprazole / Seclo
        if (query.contains("gastric") || query.contains("acidity") || query.contains("omeprazole") || query.contains("seclo") || query.contains("heartburn") || query.contains("indigestion") || query.contains("acid reflux")) {
            return "Clinical Guidance: Acid Reducers (Omeprazole / Seclo 20)\n\n" +
                   "- **Primary Uses:** Treats gastric hyperacidity, GERD, heartburn, and stomach ulcers.\n" +
                   "- **Best Timing:** Take **20 to 30 minutes before your morning meal (breakfast)** with a full glass of water.\n" +
                   "- **Dietary Advice:** Avoid skipping meals, limit spicy or oily foods, and avoid lying down immediately after eating.\n" +
                   "- **Notice:** If symptoms persist continuously for more than 2 weeks, please consult your physician.";
        }

        // Antibiotics
        if (query.contains("antibiotic") || query.contains("amoxicillin") || query.contains("azithromycin") || query.contains("infection") || query.contains("cefixime")) {
            return "Clinical Guidance on Antibiotics:\n\n" +
                   "- **Complete Your Course:** Always finish the entire prescribed course, even if you feel better after a few days.\n" +
                   "- **Prevent Resistance:** Stopping early allows bacteria to develop antibiotic resistance.\n" +
                   "- **Side Effects:** Mild stomach upset can occur. Drink plenty of water and eat yogurt or probiotics.\n" +
                   "- **Safety Rule:** Never use leftover antibiotics or share them without a doctor's prescription.";
        }

        // Drug Interactions
        if (query.contains("interaction") || query.contains("together") || query.contains("combine") || (query.contains("and") && (query.contains("take") || query.contains("can i")))) {
            return "Drug Interaction Review:\n\n" +
                   "- Common pairings like **Paracetamol + Omeprazole** have no known harmful interactions.\n" +
                   "- **Caution:** Avoid taking NSAIDs (Ibuprofen, Naproxen) with blood thinners (Aspirin) without physician clearance.\n" +
                   "- MediLink automatically checks your uploaded prescriptions for interactions in the **Prescriptions & Meds** tab.";
        }

        // Missed Dose
        if (query.contains("missed dose") || query.contains("forgot to take") || query.contains("skip")) {
            return "Protocol for a Missed Medication Dose:\n\n" +
                   "- **General Rule:** Take the missed dose as soon as you remember.\n" +
                   "- **If Near Next Dose:** Skip the missed dose and continue your normal schedule.\n" +
                   "- **Warning:** **Never take a double dose** to compensate for a missed one.";
        }

        // --------------------------------------------------------
        // UNIVERSAL LANGUAGE FALLBACK
        // Non-English input not matched by script detection above
        // --------------------------------------------------------
        if (!isLikelyEnglish(query)) {
            return "I detected your message in another language. I am MediLink and I support multiple languages.\n\n" +
                   "For full multilingual AI responses including Bengali, Arabic, Hindi, Spanish, French and more, please connect your free Google Gemini API key using the settings button in the chat header.\n\n" +
                   "With Gemini AI connected, I will automatically reply in the same language you write in.";
        }

        // English context-aware fallback
        return "I understand your question about **\"" + raw + "\"**.\n\n" +
               "As your clinical assistant, I recommend:\n" +
               "- If this is related to a prescribed drug, check your dosage instructions in your **Medical Records** tab.\n" +
               "- If you have acute or worsening symptoms, please consult a healthcare professional or contact a pharmacist via the **Pharmacist Live Chat** tab.\n\n" +
               "Tip: Connect your free Google Gemini API key in the settings to enable full AI reasoning in any language!";
    }

    // --------------------------------------------------------
    // LANGUAGE DETECTION HELPERS
    // --------------------------------------------------------

    private boolean containsBengali(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x0980 && c <= 0x09FF) return true;
        }
        return false;
    }

    private boolean containsArabic(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x0600 && c <= 0x06FF) return true;
        }
        return false;
    }

    private boolean containsDevanagari(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x0900 && c <= 0x097F) return true;
        }
        return false;
    }

    private boolean isLikelyEnglish(String query) {
        // A rough heuristic: if most chars are ASCII, treat as likely English
        int ascii = 0;
        for (char c : query.toCharArray()) {
            if (c < 128) ascii++;
        }
        return query.isEmpty() || (ascii * 1.0 / query.length()) > 0.7;
    }

    private boolean isGreeting(String q) {
        if (q.equals("hi") || q.equals("hello") || q.equals("hey") || q.equals("hi there") ||
            q.equals("hello there") || q.equals("hola") || q.equals("salam") || q.equals("assalam") ||
            q.equals("assalamu alaikum") || q.startsWith("good morning") || q.startsWith("good afternoon") ||
            q.startsWith("good evening") || q.equals("sup") || q.equals("yo")) {
            return true;
        }
        return q.matches("^(hi|hello|hey|hola|salam|hiya)[!., ]*.*$");
    }


    // --------------------------------------------------------
    // MULTILINGUAL RESPONSE BUILDERS
    // --------------------------------------------------------

    private String respondBengali(String query, String raw) {

        // 1. Greetings
        if (query.contains("হ্যালো") || query.contains("হেলো") ||
            query.contains("সালাম") || query.contains("আস্সালামু") ||
            query.contains("নমস্কার") || query.equals("হায়")) {
            return "আস্সালামু আলাইকুম! আমি **MediLink**, আপনার ২৪/৭ স্বাস্থ্যসেবা সহকারী।\n\n" +
                   "আপনি আমাকে যেকোনো বিষয়ে জিজ্ঞাসা করতে পারেন:\n" +
                   "- ওষুধের ডোজ ও সময়সূচী\n" +
                   "- পার্শ্বপ্রতিক্রিয়া ও ওষুধের মিথস্ক্রিয়া\n" +
                   "- জ্বর, মাথাব্যথা, গ্যাস্ট্রিক বা অন্য উপসর্গ\n\n" +
                   "আজ আমি আপনার কীভাবে সাহায্য করতে পারি?";
        }

        // 2. How are you: কেমন আছেন / কেমন আছো / কেমন আছ / কেমন চলছে
        if (query.contains("কেমন আছ") || query.contains("কেমন আছে") ||
            query.contains("কেমন থাক") || query.contains("কেমন চলছে") ||
            query.contains("কেমনে আছ") || query.contains("কেমন আসেন")) {
            return "আলহামদুলিল্লাহ, আমি ভালো আছি! আপনাকে সাহায্য করতে সর্বদা প্রস্তুত।\n\n" +
                   "আপনি কেমন আছেন? কোনো শারীরিক সমস্যা বা ওষুধ সম্পর্কিত কোনো প্রশ্ন আছে?";
        }

        // 3. Who are you
        if (query.contains("আপনি কে") || query.contains("তুমি কে") ||
            query.contains("আপনি কী") || query.contains("আপনি কি করেন")) {
            return "আমি **MediLink**, MediLink 2.0 প্ল্যাটফর্মের একটি বুদ্ধিমান স্বাস্থ্যসেবা সহকারী।\n\n" +
                   "আমি আপনাকে সাহায্য করতে পারি:\n" +
                   "- প্রেসক্রিপশন বা ওষুধের ডোজ বোঝার জন্য\n" +
                   "- ওষুধের পার্শ্বপ্রতিক্রিয়া যাচাই করার জন্য\n" +
                   "- জ্বর, মাথাব্যথা বা গ্যাস্ট্রিকের সমাধান\n\n" +
                   "যেকোনো প্রশ্ন করুন!";
        }

        // 4. Thank you
        if (query.contains("ধন্যবাদ") || query.contains("শুকরিয়া") || query.contains("থ্যাংকস")) {
            return "আপনাকে স্বাগতম! আপনার স্বাস্থ্য ও নিরাপত্তা আমাদের সর্বোচ্চ অবস্থানের বিষয়।\n\n" +
                   "আর কোনো প্রশ্ন থাকলে যেকোনো সময় জিজ্ঞাসা করুন!";
        }

        // 5. Goodbye
        if (query.contains("বিদায়") || query.contains("আল্লাহ হাফেজ") ||
            query.contains("খোদা হাফেজ") || query.contains("আবার দেখা")) {
            return "আল্লাহ হাফেজ! সুস্থ থাকুন এবং নিয়মিত ওষুধ খান্না ভুলবেন না। যেকোনো সময় আমি আপনার পাশে আছি!";
        }

        // 6. Emergency
        if (query.contains("বুকে ব্যথা") || query.contains("হার্ট অ্যাটাক") || query.contains("শ্বাস নিতে পারছি না")) {
            return "সতর্কতা! এটি জরুরি চিকিৎসা পরিস্থিতি হতে পারে।\n\n" +
                   "এখনই **১৯৯ বা অ্যাম্বুলেন্স** ডাকুন অথবা নিকটতম হাসপাতালে যান!";
        }

        // 7. Fever
        if (query.contains("জ্বর")) {
            return "জ্বরের জন্য নির্দেশনা:\n\n" +
                   "- **প্যারাসিটামল (নাপা/এস):** প্রতি ৪-৬ ঘন্টায় ৫০০মিগ্রা. খাবার পর পানি দিয়ে খান\n" +
                   "- প্রচুর পানি ও তরল জিনিস পান করুন\n" +
                   "- ৩৯ ডিগ্রির বেশি জ্বর থাকলে চিকিৎসকের সাথে যোগাযোগ করুন";
        }

        // 8. Headache
        if (query.contains("মাথা ব্যথা") || query.contains("মাথাব্যথা")) {
            return "মাথাব্যথার জন্য নির্দেশনা:\n\n" +
                   "- **প্যারাসিটামল (নাপা):** ৪-৬ ঘন্টা পর ৫০০ মিগ্রা. খাবারের পর খান\n" +
                   "- প্রচুর পানি পান করুন, বিশ্রাম নিন\n" +
                   "- দীর্ঘস্থায়ী বা প্রচণ্ড মাথাব্যথার জন্য চিকিৎসক দেখান";
        }

        // 9. Gastric / Acidity
        if (query.contains("গ্যাস্ট্রিক") || query.contains("এসিডিটি") || query.contains("বুক জ্বালা") || query.contains("হজম হচ্ছে না")) {
            return "গ্যাস্ট্রিক বা এসিডিটির জন্য নির্দেশনা:\n\n" +
                   "- **ওমিপ্রাজল (সেকলো ২০):** সকালে নাশতার ২০-৩০ মিনিট আগে পানি দিয়ে খান\n" +
                   "- তৈলাক্ত ও মশলাদার খাবার এড়িয়ে চলুন\n" +
                   "- খাওয়ার পরর সঙ্গে শুয়ে পড়বেন না\n" +
                   "- ২ সপ্তাহের বেশি সমস্যা থাকলে ডাক্তার দেখান";
        }

        // 10. Medicine question
        if (query.contains("ওষুধ") || query.contains("ট্যাবলেট") || query.contains("ক্যাপসুল") || query.contains("ডোজ") || query.contains("সিরাপ")) {
            return "ওষুধ সম্পর্কিত বিষয়ে সরাসরি ডাক্তার বা ফার্মাসিস্টের পরামর্শ নিন।\n\n" +
                   "কোন নির্দিষ্ট ওষুধের নাম দিন, আমি ডোজ ও ব্যবহারের নির্দেশনা দিতে পারব!\n\n" +
                   "যেমন: নাপা, সেকলো, অ্যামোক্সিসিলিন, ইনসুলিন ইত্যাদি";
        }

        // Generic Bengali fallback
        return "আমি আপনার প্রশ্নটি বুঝতে পেরেছি। আরও বিস্তারিত সাড়া পেতে আপনার প্রশ্নটি আরো স্পষ্টকরে দিন।\n\n" +
               "নিম্নলিখিত বিষয়ে জিজ্ঞাসা করতে পারেন:\n" +
               "- ওষুধের ডোজ ও সময় (যেমন: নাপা, সেকলো)\n" +
               "- জ্বর, মাথাব্যথা বা গ্যাস্ট্রিকের সমাধান\n" +
               "- ওষুধের পার্শ্বপ্রতিক্রিয়া যাচাই";
    }

    private String respondArabic(String query, String raw) {
        if (query.contains("كيف حالك") || query.contains("كيف حال")) {
            return "بخير، شكراً! أنا هنا 24/7 لمساعدتك.\n\nكيف تشعر اليوم؟ هل لديك أي أسئلة حول صحتك أو أدويتك؟";
        }
        return "مرحباً! أنا **MediLink**، مساعدك الصحي الشخصي المتاح على مدار الساعة 24/7.\n\n" +
               "يمكنني مساعدتك في:\n" +
               "- جرعات الأدوية وتوقيتها\n" +
               "- التفاعلات الدوائية والآثار الجانبية\n" +
               "- أعراض مثل الحمى والصداع وحموضة المعدة\n\n" +
               "كيف يمكنني مساعدتك اليوم؟";
    }

    private String respondHindi(String query, String raw) {
        if (query.contains("कैसे हैं") || query.contains("कैसे हो") || query.contains("कैसे है")) {
            return "मैं ठीक हूँ, शुक्रिया! आपकी मदद के लिए हमेशा तैयार हूँ.\n\nआप कैसा महसूस कर रहे हैं? क्या कोई स्वास्थ्य संबंधी प्रश्न है?";
        }
        return "नमस्ते! मैं **MediLink** हूं, आपका 24/7 व्यक्तिगत स्वास्थ्य सहायक।\n\n" +
               "आप मुझसे इन विषयों पर पूछ सकते हैं:\n" +
               "- दवाओं की खुराक और समय\n" +
               "- दवाओं के दुष्प्रभाव और इंटरेक्शन\n" +
               "- बुखार, सिरदर्द, एसिडिटी जैसे लक्षण\n\n" +
               "आज मैं आपकी कैसे सहायता कर सकता हूं?";
    }
}
