package com.medilink.service;

import com.medilink.model.chat.ChatMessage;
import com.medilink.model.user.Patient;
import com.medilink.model.user.Pharmacist;
import com.medilink.repository.ChatMessageRepository;
import com.medilink.repository.PatientRepository;
import com.medilink.repository.PharmacistRepository;
import com.medilink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final StockObserverService stockObserverService;

    @Autowired
    public ChatService(ChatMessageRepository chatMessageRepository,
                       PharmacistRepository pharmacistRepository,
                       PatientRepository patientRepository,
                       UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.stockObserverService = StockObserverService.getInstance();
    }

    public List<ChatMessage> getHistory(String user1, String user2) {
        List<String> ids1 = resolveUserAliases(user1);
        List<String> emails1 = resolveEmails(user1);
        List<String> ids2 = resolveUserAliases(user2);
        List<String> emails2 = resolveEmails(user2);

        if (ids1.isEmpty()) ids1 = Collections.singletonList(user1 != null ? user1 : "__NONE__");
        if (emails1.isEmpty()) emails1 = Collections.singletonList("__NONE__");
        if (ids2.isEmpty()) ids2 = Collections.singletonList(user2 != null ? user2 : "__NONE__");
        if (emails2.isEmpty()) emails2 = Collections.singletonList("__NONE__");

        List<ChatMessage> list = chatMessageRepository.findChatHistoryFlexible(ids1, emails1, ids2, emails2);
        if (list.isEmpty()) {
            list = chatMessageRepository.findChatHistory(user1, user2);
        }
        return list;
    }

    /**
     * Returns all registered pharmacists from the database so that any newly
     * created pharmacist account immediately appears in the Live Chat sidebar.
     *
     * Seed pharmacists (usr_pharma_01, usr_pharma_02) are always listed first
     * with their full metadata. All additional pharmacists registered afterwards
     * are appended dynamically from the pharmacists table.
     */
    public List<Map<String, Object>> getAvailablePharmacists() {
        List<Map<String, Object>> list = new ArrayList<>();

        // Seed well-known pharmacists with full rich metadata
        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", "usr_pharma_01");
        p1.put("aliasId", "PH-9920-DGDA");
        p1.put("name", "Dr. Farhan Kabir");
        p1.put("role", "PHARMACIST");
        p1.put("email", "farhan@lazzpharma.com");
        p1.put("pharmacy", "Lazz Pharma (Dhanmondi Branch)");
        p1.put("license", "DGDA-PH-9920");
        p1.put("avatar", "🩺");
        p1.put("isOnline", true);
        p1.put("statusText", "Online • Ready for Consultation");
        p1.put("specialties", Arrays.asList("Pharmacotherapy", "Drug Interactions", "Dose Monitoring"));
        list.add(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", "usr_pharma_02");
        p2.put("aliasId", "PH-8411-DGDA");
        p2.put("name", "Dr. Nazmul Huda");
        p2.put("role", "PHARMACIST");
        p2.put("email", "nazmul@popularpharma.com");
        p2.put("pharmacy", "Popular Pharmacy (Gulshan 2 Branch)");
        p2.put("license", "DGDA-PH-8411");
        p2.put("avatar", "👨‍⚕️");
        p2.put("isOnline", true);
        p2.put("statusText", "Online • Available");
        p2.put("specialties", Arrays.asList("Antibiotic Stewardship", "Chronic Care Management"));
        list.add(p2);

        // IDs already added above — skip them when iterating the DB
        Set<String> seedIds = new HashSet<>(Arrays.asList("usr_pharma_01", "usr_pharma_02"));

        try {
            List<Pharmacist> dbPharmacists = pharmacistRepository.findAll();
            for (Pharmacist ph : dbPharmacists) {
                if (ph.getId() == null || seedIds.contains(ph.getId())) {
                    continue; // Skip seed pharmacists already added with rich metadata
                }
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", ph.getId());
                entry.put("aliasId", ph.getId()); // dynamic pharmacist IDs are self-aliased
                entry.put("name", ph.getName() != null ? ph.getName() : "Pharmacist");
                entry.put("role", "PHARMACIST");
                entry.put("email", ph.getEmail() != null ? ph.getEmail().toLowerCase() : "");
                // Prefer pharmacyName stored on the entity; fall back to a sensible default
                String pharmacy = (ph.getPharmacyName() != null && !ph.getPharmacyName().trim().isEmpty())
                        ? ph.getPharmacyName()
                        : "Licensed Pharmacy";
                entry.put("pharmacy", pharmacy);
                String license = (ph.getLicenseNumber() != null && !ph.getLicenseNumber().trim().isEmpty())
                        ? ph.getLicenseNumber()
                        : "DGDA-LICENSED";
                entry.put("license", license);
                entry.put("avatar", "💊");
                entry.put("isOnline", true);
                entry.put("statusText", "Online • Available for Consultation");
                entry.put("specialties", Arrays.asList("General Pharmacy", "Clinical Consultation"));
                list.add(entry);
            }
        } catch (Exception e) {
            // If DB is unavailable, return the seed list so the UI always has pharmacists
            System.err.println("[ChatService] Warning: Could not load pharmacists from DB: " + e.getMessage());
        }

        return list;
    }

    public ChatMessage saveMessage(String senderId, String senderName, String senderRole,
                                   String receiverId, String content, String type) {
        return saveMessage(senderId, senderName, senderRole, null, receiverId, null, content, type, null, null, false);
    }

    public ChatMessage saveMessage(String senderId, String senderName, String senderRole, String senderEmail,
                                   String receiverId, String receiverEmail, String content, String type,
                                   String prescriptionId, String prescriptionSummary, boolean autoReply) {

        ChatMessage msg = new ChatMessage(senderId, senderName, senderRole, senderEmail,
                receiverId, receiverEmail, content, type, prescriptionId, prescriptionSummary);
        ChatMessage saved = chatMessageRepository.save(msg);

        // Notify client streams over SSE
        broadcastChatEvent(saved);

        // Trigger contextual clinical response only if explicitly requested (e.g. quick inquiry chips)
        if (autoReply) {
            triggerAutomatedClinicalResponse(saved);
        }

        return saved;
    }

    private void broadcastChatEvent(ChatMessage msg) {
        String json = String.format("{\"type\":\"CHAT_MESSAGE\",\"id\":\"%s\",\"senderId\":\"%s\",\"senderName\":\"%s\",\"senderRole\":\"%s\",\"receiverId\":\"%s\",\"receiverEmail\":\"%s\",\"content\":\"%s\",\"prescriptionId\":%s,\"timestamp\":\"%s\"}",
                msg.getId() != null ? msg.getId() : "",
                msg.getSenderId(),
                escapeJson(msg.getSenderName()),
                msg.getSenderRole(),
                msg.getReceiverId(),
                escapeJson(msg.getReceiverEmail() != null ? msg.getReceiverEmail() : ""),
                escapeJson(msg.getContent()),
                msg.getPrescriptionId() != null ? "\"" + escapeJson(msg.getPrescriptionId()) + "\"" : "null",
                msg.getTimestamp() != null ? msg.getTimestamp().toString() : LocalDateTime.now().toString()
        );
        stockObserverService.onNotification("CHAT_MESSAGE", json);
    }

    private void triggerAutomatedClinicalResponse(ChatMessage patientMsg) {
        // Capture the pharmacist data before going async (avoids lazy-load issues)
        final String pharmaId = (patientMsg.getReceiverId() != null && !patientMsg.getReceiverId().isEmpty())
                ? patientMsg.getReceiverId()
                : "usr_pharma_01";

        // Resolve pharmacist name and store from DB; fall back to known seed data
        final String resolvedPharmaId;
        final String pharmaEmail;
        final String pharmaName;
        final String storeName;

        if ("usr_pharma_02".equalsIgnoreCase(pharmaId) ||
            (patientMsg.getReceiverEmail() != null && patientMsg.getReceiverEmail().contains("nazmul")) ||
            (patientMsg.getContent() != null && patientMsg.getContent().toLowerCase().contains("nazmul"))) {
            resolvedPharmaId = "usr_pharma_02";
            pharmaEmail = "nazmul@popularpharma.com";
            pharmaName = "Dr. Nazmul Huda";
            storeName = "Popular Pharmacy (Gulshan 2 Branch)";
        } else if ("usr_pharma_01".equalsIgnoreCase(pharmaId) || pharmaId.contains("farhan")) {
            resolvedPharmaId = "usr_pharma_01";
            pharmaEmail = "farhan@lazzpharma.com";
            pharmaName = "Dr. Farhan Kabir";
            storeName = "Lazz Pharma (Dhanmondi Branch)";
        } else {
            // Dynamically registered pharmacist — look up from the database
            Optional<Pharmacist> dbPharma = pharmacistRepository.findById(pharmaId);
            if (dbPharma.isPresent()) {
                Pharmacist ph = dbPharma.get();
                resolvedPharmaId = ph.getId();
                pharmaEmail = ph.getEmail() != null ? ph.getEmail() : "";
                pharmaName = ph.getName() != null ? ph.getName() : "Pharmacist";
                storeName = (ph.getPharmacyName() != null && !ph.getPharmacyName().trim().isEmpty())
                        ? ph.getPharmacyName()
                        : "Licensed Pharmacy";
            } else {
                // Unknown pharmacist ID — use the default seed pharmacist
                resolvedPharmaId = "usr_pharma_01";
                pharmaEmail = "farhan@lazzpharma.com";
                pharmaName = "Dr. Farhan Kabir";
                storeName = "Lazz Pharma (Dhanmondi Branch)";
            }
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1200); // Realistic clinical typing delay
            } catch (InterruptedException ignored) {}

            String replyText = generateClinicalAdvice(patientMsg, pharmaName, storeName);

            ChatMessage reply = new ChatMessage(
                    resolvedPharmaId,
                    pharmaName,
                    "PHARMACIST",
                    pharmaEmail,
                    patientMsg.getSenderId(),
                    patientMsg.getSenderEmail(),
                    replyText,
                    "CLINICAL_ADVICE",
                    patientMsg.getPrescriptionId(),
                    null
            );
            chatMessageRepository.save(reply);
            broadcastChatEvent(reply);
        });
    }

    private String generateClinicalAdvice(ChatMessage msg, String pharmaName, String storeName) {
        String sender = (msg.getSenderName() != null && !msg.getSenderName().trim().isEmpty())
                ? msg.getSenderName() : "Patient";

        return "Dear " + sender + ", thank you very much for sending the message. Someone will contact you shortly!";
    }

    public void markMessagesAsRead(String user1, String user2) {
        List<ChatMessage> list = getHistory(user1, user2);
        for (ChatMessage m : list) {
            if (m.getIsRead() == null || !m.getIsRead()) {
                m.setIsRead(true);
                chatMessageRepository.save(m);
            }
        }
    }

    /**
     * Retrieves rich conversation threads for the active user.
     * If user is a PHARMACIST: returns list of all patient threads with unread counts,
     * latest message snippets, timestamps, and attached prescription indicators.
     * If user is a PATIENT: returns available pharmacists with latest message history and unread counts.
     */
    public List<Map<String, Object>> getConversations(String userId, String role) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> userIds = resolveUserAliases(userId);
        List<String> userEmails = resolveEmails(userId);
        if (userIds.isEmpty()) userIds = Collections.singletonList(userId != null ? userId : "__NONE__");
        if (userEmails.isEmpty()) userEmails = Collections.singletonList("__NONE__");

        boolean isPharmacist = "PHARMACIST".equalsIgnoreCase(role)
                || pharmacistRepository.existsById(userId != null ? userId : "")
                || (userId != null && (userId.toLowerCase().startsWith("usr_pharma") || userId.toUpperCase().startsWith("PH-")));

        if (isPharmacist) {
            List<ChatMessage> messages = chatMessageRepository.findMessagesInvolvingUser(userIds, userEmails);

            Map<String, List<ChatMessage>> threadMap = new LinkedHashMap<>();
            Map<String, String> nameMap = new HashMap<>();
            Map<String, String> emailMap = new HashMap<>();

            for (ChatMessage m : messages) {
                String partnerId;
                String partnerName;
                String partnerEmail;

                if ("PATIENT".equalsIgnoreCase(m.getSenderRole())) {
                    partnerId = m.getSenderId();
                    partnerName = m.getSenderName();
                    partnerEmail = m.getSenderEmail();
                } else {
                    partnerId = m.getReceiverId();
                    partnerName = m.getReceiverId();
                    partnerEmail = m.getReceiverEmail();
                }

                if (partnerId == null || partnerId.trim().isEmpty() || userIds.contains(partnerId)) {
                    continue;
                }

                threadMap.computeIfAbsent(partnerId, k -> new ArrayList<>()).add(m);
                if (partnerName != null && !partnerName.isEmpty() && !partnerName.equals(partnerId)) {
                    nameMap.put(partnerId, partnerName);
                }
                if (partnerEmail != null && !partnerEmail.isEmpty()) {
                    emailMap.put(partnerId, partnerEmail);
                }
            }

            // Also ensure registered patients exist in thread list
            try {
                List<Patient> allPatients = patientRepository.findAll();
                for (Patient p : allPatients) {
                    if (p.getId() != null && !threadMap.containsKey(p.getId())) {
                        threadMap.put(p.getId(), new ArrayList<>());
                        nameMap.put(p.getId(), p.getName());
                        emailMap.put(p.getId(), p.getEmail());
                    }
                }
            } catch (Exception ignored) {}

            // Ensure seed patient Rahim Ahmed (ML-9824-A) is always available
            if (!threadMap.containsKey("ML-9824-A")) {
                threadMap.put("ML-9824-A", new ArrayList<>());
                nameMap.put("ML-9824-A", "Rahim Ahmed");
                emailMap.put("ML-9824-A", "rahim@medilink.com");
            }

            for (Map.Entry<String, List<ChatMessage>> entry : threadMap.entrySet()) {
                String pId = entry.getKey();
                List<ChatMessage> pMsgs = entry.getValue();

                Map<String, Object> conv = new HashMap<>();
                conv.put("partnerId", pId);
                String resolvedName = nameMap.get(pId);
                if (resolvedName == null || resolvedName.isEmpty()) {
                    try {
                        Optional<Patient> opt = patientRepository.findById(pId);
                        resolvedName = opt.map(Patient::getName).orElse("Patient (" + pId + ")");
                    } catch (Exception e) {
                        resolvedName = "Patient (" + pId + ")";
                    }
                }
                conv.put("partnerName", resolvedName);
                conv.put("partnerRole", "PATIENT");
                conv.put("partnerEmail", emailMap.getOrDefault(pId, "rahim@medilink.com"));
                conv.put("avatar", "👤");
                conv.put("isOnline", true);
                conv.put("statusText", "Patient • Tele-Consultation Active");

                long unread = pMsgs.stream().filter(m -> 
                    "PATIENT".equalsIgnoreCase(m.getSenderRole()) && (m.getIsRead() == null || !m.getIsRead())
                ).count();
                conv.put("unreadCount", unread);

                if (!pMsgs.isEmpty()) {
                    ChatMessage latest = pMsgs.get(0); // ordered DESC by timestamp
                    conv.put("lastMessage", latest.getContent());
                    conv.put("lastMessageTime", latest.getTimestamp() != null ? latest.getTimestamp().toString() : "");
                    conv.put("prescriptionId", latest.getPrescriptionId());
                    conv.put("prescriptionSummary", latest.getPrescriptionSummary());
                    conv.put("timestampRaw", latest.getTimestamp());
                } else {
                    conv.put("lastMessage", "Ready for clinical consultation");
                    conv.put("lastMessageTime", "");
                    conv.put("prescriptionId", null);
                    conv.put("prescriptionSummary", null);
                    conv.put("timestampRaw", LocalDateTime.of(2000, 1, 1, 0, 0));
                }

                result.add(conv);
            }

            // Sort: highest unread first, then latest message timestamp descending
            result.sort((a, b) -> {
                long uA = ((Number) a.getOrDefault("unreadCount", 0L)).longValue();
                long uB = ((Number) b.getOrDefault("unreadCount", 0L)).longValue();
                if (uA != uB) return Long.compare(uB, uA);

                LocalDateTime tA = (LocalDateTime) a.getOrDefault("timestampRaw", LocalDateTime.MIN);
                LocalDateTime tB = (LocalDateTime) b.getOrDefault("timestampRaw", LocalDateTime.MIN);
                return tB.compareTo(tA);
            });

            result.forEach(m -> m.remove("timestampRaw"));

        } else {
            // Patient perspective: return available pharmacists with their latest chat status
            List<Map<String, Object>> pharmacists = getAvailablePharmacists();
            for (Map<String, Object> pharma : pharmacists) {
                String pharmaId = String.valueOf(pharma.get("id"));
                List<String> pharmaAliases = resolveUserAliases(pharmaId);
                List<String> pharmaEmails = resolveEmails(pharmaId);
                if (pharmaAliases.isEmpty()) pharmaAliases = Collections.singletonList(pharmaId);
                if (pharmaEmails.isEmpty()) pharmaEmails = Collections.singletonList("__NONE__");

                List<ChatMessage> hist = chatMessageRepository.findChatHistoryFlexible(userIds, userEmails, pharmaAliases, pharmaEmails);
                long unread = hist.stream().filter(m -> 
                    "PHARMACIST".equalsIgnoreCase(m.getSenderRole()) && (m.getIsRead() == null || !m.getIsRead())
                ).count();

                Map<String, Object> conv = new HashMap<>(pharma);
                conv.put("partnerId", pharmaId);
                conv.put("partnerName", pharma.get("name"));
                conv.put("partnerRole", "PHARMACIST");
                conv.put("partnerEmail", pharma.get("email"));
                conv.put("unreadCount", unread);

                if (!hist.isEmpty()) {
                    ChatMessage latest = hist.get(hist.size() - 1);
                    conv.put("lastMessage", latest.getContent());
                    conv.put("lastMessageTime", latest.getTimestamp() != null ? latest.getTimestamp().toString() : "");
                    conv.put("prescriptionId", latest.getPrescriptionId());
                } else {
                    conv.put("lastMessage", pharmaId.equals("usr_pharma_02") ? "Antibiotic & chronic care consultation" : "Maxpro, Napa & PPI guidance");
                    conv.put("lastMessageTime", "");
                    conv.put("prescriptionId", null);
                }
                result.add(conv);
            }
        }

        return result;
    }

    public long getUnreadCount(String userId) {
        List<String> ids = resolveUserAliases(userId);
        List<String> emails = resolveEmails(userId);
        if (ids.isEmpty()) ids = Collections.singletonList(userId != null ? userId : "__NONE__");
        if (emails.isEmpty()) emails = Collections.singletonList("__NONE__");
        return chatMessageRepository.countUnreadFlexible(ids, emails);
    }

    private List<String> resolveUserAliases(String user) {
        Set<String> aliases = new HashSet<>();
        if (user != null && !user.trim().isEmpty()) {
            String u = user.trim();
            aliases.add(u);
            String lower = u.toLowerCase();
            if (lower.equals("usr_pharma_01") || lower.equals("ph-9920-dgda") || (lower.contains("farhan") && !lower.contains("nazmul"))) {
                aliases.addAll(Arrays.asList("usr_pharma_01", "PH-9920-DGDA"));
            } else if (lower.equals("usr_pharma_02") || lower.equals("ph-8411-dgda") || (lower.contains("nazmul") && !lower.contains("farhan"))) {
                aliases.addAll(Arrays.asList("usr_pharma_02", "PH-8411-DGDA"));
            } else if (lower.equals("ml-9824-a") || lower.equals("pa-9824-a") || lower.equals("usr_patient_01") || lower.contains("rahim") || lower.contains("maisha")) {
                aliases.addAll(Arrays.asList("ML-9824-A", "PA-9824-A", "usr_patient_01"));
            }

            try {
                if (userRepository != null) {
                    userRepository.findById(u).ifPresent(foundUser -> aliases.add(foundUser.getId()));
                    userRepository.findByEmailIgnoreCase(u).ifPresent(foundUser -> aliases.add(foundUser.getId()));
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(aliases);
    }

    private List<String> resolveEmails(String user) {
        Set<String> emails = new HashSet<>();
        if (user != null && !user.trim().isEmpty()) {
            String u = user.trim().toLowerCase();
            if (u.contains("@")) {
                emails.add(u);
            }
            if (u.equals("usr_pharma_01") || (u.contains("farhan") && !u.contains("nazmul")) || u.equals("ph-9920-dgda")) {
                emails.add("farhan@lazzpharma.com");
            } else if (u.equals("usr_pharma_02") || (u.contains("nazmul") && !u.contains("farhan")) || u.equals("ph-8411-dgda")) {
                emails.add("nazmul@popularpharma.com");
            } else if (u.equals("ml-9824-a") || u.equals("pa-9824-a") || u.equals("usr_patient_01") || u.contains("rahim") || u.contains("maisha")) {
                emails.add("rahim@medilink.com");
            }

            try {
                if (userRepository != null) {
                    userRepository.findById(user.trim()).ifPresent(usr -> {
                        if (usr.getEmail() != null) emails.add(usr.getEmail().toLowerCase());
                    });
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(emails);
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
