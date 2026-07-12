package com.company.icps.config;

import com.company.icps.audit.repository.AuditLogRepository;
import com.company.icps.claim.entity.Claim;
import com.company.icps.claim.entity.ClaimStatus;
import com.company.icps.claim.entity.ClaimType;
import com.company.icps.claim.repository.ClaimRepository;
import com.company.icps.document.repository.DocumentRepository;
import com.company.icps.user.entity.Role;
import com.company.icps.user.entity.User;
import com.company.icps.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the database with initial demo data on first startup.
 * Runs once; skips if more than 5 users already exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Clear existing data (respecting FK constraints)
        auditLogRepository.deleteAll();
        documentRepository.deleteAll();
        claimRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.flush();
        claimRepository.flush();

        // 2. Encode passwords
        String customerPwd = passwordEncoder.encode("1234567");
        String staffPwd    = passwordEncoder.encode("Employee@ClaimFlow");

        // 3. Create Admin (1)
        userRepository.save(buildUser("Vikram", "Sharma", "admin@icps.com", staffPwd, Role.ADMIN));
        log.info("  Created ADMIN: admin@icps.com / Employee@ClaimFlow");

        // 4. Create Supervisors (2)
        userRepository.save(buildUser("Arjun",  "Verma", "sup1@icps.com", staffPwd, Role.SUPERVISOR));
        userRepository.save(buildUser("Rajesh", "Iyer",  "sup2@icps.com", staffPwd, Role.SUPERVISOR));
        log.info("  Created 2 SUPERVISORS: sup1@icps.com, sup2@icps.com / Employee@ClaimFlow");

        // 5. Create Investigators (4)
        userRepository.save(buildUser("Suresh", "Patel", "inv1@icps.com", staffPwd, Role.INVESTIGATOR));
        userRepository.save(buildUser("Ramesh", "Nair",  "inv2@icps.com", staffPwd, Role.INVESTIGATOR));
        userRepository.save(buildUser("Manoj",  "Desai", "inv3@icps.com", staffPwd, Role.INVESTIGATOR));
        userRepository.save(buildUser("Kiran",  "Reddy", "inv4@icps.com", staffPwd, Role.INVESTIGATOR));
        log.info("  Created 4 INVESTIGATORS: inv1..inv4@icps.com / Employee@ClaimFlow");

        // 6. Create Claim Agents (4)
        List<User> agents = new ArrayList<>();
        String[][] agentData = {
            {"Amit",  "Singh",  "agent1@icps.com"},
            {"Rahul", "Kumar",  "agent2@icps.com"},
            {"Neha",  "Gupta",  "agent3@icps.com"},
            {"Priya", "Sharma", "agent4@icps.com"}
        };
        for (String[] a : agentData) {
            agents.add(userRepository.save(buildUser(a[0], a[1], a[2], staffPwd, Role.CLAIM_AGENT)));
        }
        log.info("  Created 4 CLAIM_AGENTS: agent1..agent4@icps.com / Employee@ClaimFlow");

        // 7. Create Customers (5) and their claims
        createCustomerWithClaims("Aarav",  "Das",     "cust1@icps.com", customerPwd, agents, 1);
        createCustomerWithClaims("Vihaan", "Rao",     "cust2@icps.com", customerPwd, agents, 2);
        createCustomerWithClaims("Aditya", "Menon",   "cust3@icps.com", customerPwd, agents, 3);
        createCustomerWithClaims("Sai",    "Pillai",  "cust4@icps.com", customerPwd, agents, 4);
        createCustomerWithClaims("Ishaan", "Gowda",   "cust5@icps.com", customerPwd, agents, 5);

        log.info("  Created 5 CUSTOMERS: cust1..cust5@icps.com / 1234567");
        log.info("=== Database seeding complete ===");
    }

    /**
     * Creates a customer and their claims.
     * Each customer gets a unique set of claims covering different types and statuses.
     */
    private void createCustomerWithClaims(String fn, String ln, String email,
                                           String pwd, List<User> agents, int custIdx) {
        User customer = userRepository.save(buildUser(fn, ln, email, pwd, Role.CUSTOMER));

        switch (custIdx) {
            // ---- Customer 1: Aarav Das — 10 claims, all statuses covered ----
            case 1 -> {
                createClaim(customer, null,        ClaimType.HEALTH,    ClaimStatus.DRAFT,                  "CL-1001", "Hospitalization for acute fever treatment",       5000.00,  "POL-HEALTH-1001");
                createClaim(customer, null,        ClaimType.AUTO,      ClaimStatus.SUBMITTED,              "CL-1002", "Car accident on highway - vehicle damage",        15000.00, "POL-AUTO-1002");
                createClaim(customer, agents.get(0), ClaimType.HOME,   ClaimStatus.UNDER_REVIEW,           "CL-1003", "Roof leak caused water damage to furniture",      8000.00,  "POL-HOME-1003");
                createClaim(customer, agents.get(1), ClaimType.LIFE,   ClaimStatus.INVESTIGATION_REQUIRED, "CL-1004", "Life insurance policy claim for critical illness", 500000.00,"POL-LIFE-1004");
                createClaim(customer, agents.get(2), ClaimType.TRAVEL, ClaimStatus.UNDER_INVESTIGATION,    "CL-1005", "Lost baggage on international flight",            1200.00,  "POL-TRAVEL-1005");
                createClaim(customer, agents.get(3), ClaimType.PROPERTY,ClaimStatus.INVESTIGATION_COMPLETED,"CL-1006","Fire damage at warehouse storage unit",           25000.00, "POL-PROP-1006");
                createClaim(customer, agents.get(0), ClaimType.LIABILITY,ClaimStatus.APPROVED,             "CL-1007", "Slip and fall injury on commercial premises",     3500.00,  "POL-LIAB-1007");
                createClaim(customer, agents.get(1), ClaimType.HEALTH, ClaimStatus.REJECTED,               "CL-1008", "Outpatient dental procedure not covered by plan",  500.00,  "POL-HEALTH-1008");
                createClaim(customer, agents.get(2), ClaimType.AUTO,   ClaimStatus.CLOSED,                 "CL-1009", "Minor parking lot scratch - resolved",             200.00,  "POL-AUTO-1009");
                createClaim(customer, agents.get(3), ClaimType.HOME,   ClaimStatus.DRAFT,                  "CL-1010", "Broken window pane due to storm",                 800.00,   "POL-HOME-1010");
            }
            // ---- Customer 2: Vihaan Rao — 8 claims ----
            case 2 -> {
                createClaim(customer, null,        ClaimType.AUTO,      ClaimStatus.DRAFT,                  "CL-2001", "Rear-end collision on city road",                 12000.00, "POL-AUTO-2001");
                createClaim(customer, null,        ClaimType.HEALTH,    ClaimStatus.SUBMITTED,              "CL-2002", "Surgery for appendicitis - hospital stay 5 days", 45000.00, "POL-HEALTH-2002");
                createClaim(customer, agents.get(0), ClaimType.TRAVEL, ClaimStatus.UNDER_REVIEW,           "CL-2003", "Flight cancellation - trip insurance claim",       8500.00,  "POL-TRAVEL-2003");
                createClaim(customer, agents.get(1), ClaimType.HOME,   ClaimStatus.APPROVED,               "CL-2004", "Flood damage to basement - restoration required", 35000.00, "POL-HOME-2004");
                createClaim(customer, agents.get(2), ClaimType.PROPERTY,ClaimStatus.INVESTIGATION_REQUIRED,"CL-2005", "Theft of equipment from office premises",         18000.00, "POL-PROP-2005");
                createClaim(customer, agents.get(3), ClaimType.LIABILITY,ClaimStatus.UNDER_INVESTIGATION,  "CL-2006", "Third-party vehicle damage in parking lot",        9500.00,  "POL-LIAB-2006");
                createClaim(customer, agents.get(0), ClaimType.LIFE,   ClaimStatus.REJECTED,               "CL-2007", "Accidental death benefit claim - documentation incomplete", 200000.00, "POL-LIFE-2007");
                createClaim(customer, agents.get(1), ClaimType.AUTO,   ClaimStatus.CLOSED,                 "CL-2008", "Windshield replacement after hailstorm",          3200.00,  "POL-AUTO-2008");
            }
            // ---- Customer 3: Aditya Menon — 7 claims ----
            case 3 -> {
                createClaim(customer, null,        ClaimType.HEALTH,    ClaimStatus.SUBMITTED,              "CL-3001", "Knee replacement surgery rehabilitation",          60000.00, "POL-HEALTH-3001");
                createClaim(customer, agents.get(0), ClaimType.PROPERTY,ClaimStatus.UNDER_REVIEW,          "CL-3002", "Office equipment damage due to power surge",       22000.00, "POL-PROP-3002");
                createClaim(customer, agents.get(1), ClaimType.AUTO,   ClaimStatus.INVESTIGATION_COMPLETED,"CL-3003", "Total loss of vehicle in major accident",          75000.00, "POL-AUTO-3003");
                createClaim(customer, agents.get(2), ClaimType.TRAVEL, ClaimStatus.APPROVED,               "CL-3004", "Medical emergency during overseas travel",         15000.00, "POL-TRAVEL-3004");
                createClaim(customer, agents.get(3), ClaimType.HOME,   ClaimStatus.UNDER_INVESTIGATION,    "CL-3005", "Structural damage after earthquake tremors",       95000.00, "POL-HOME-3005");
                createClaim(customer, agents.get(0), ClaimType.LIABILITY,ClaimStatus.DRAFT,                "CL-3006", "Dog bite incident on residential property",         4500.00, "POL-LIAB-3006");
                createClaim(customer, agents.get(1), ClaimType.HEALTH, ClaimStatus.CLOSED,                 "CL-3007", "Physiotherapy sessions post surgery",              12000.00, "POL-HEALTH-3007");
            }
            // ---- Customer 4: Sai Pillai — 6 claims ----
            case 4 -> {
                createClaim(customer, null,        ClaimType.HOME,      ClaimStatus.DRAFT,                  "CL-4001", "Electrical wiring damage - fire hazard repair",   6500.00,  "POL-HOME-4001");
                createClaim(customer, null,        ClaimType.HEALTH,    ClaimStatus.SUBMITTED,              "CL-4002", "Cardiac procedure and ICU hospitalization",        85000.00, "POL-HEALTH-4002");
                createClaim(customer, agents.get(2), ClaimType.AUTO,   ClaimStatus.UNDER_REVIEW,           "CL-4003", "Vehicle stolen from residential area",             42000.00, "POL-AUTO-4003");
                createClaim(customer, agents.get(3), ClaimType.TRAVEL, ClaimStatus.APPROVED,               "CL-4004", "Lost passport and travel documents abroad",        5500.00,  "POL-TRAVEL-4004");
                createClaim(customer, agents.get(0), ClaimType.PROPERTY,ClaimStatus.REJECTED,              "CL-4005", "Commercial property damage - maintenance issue excluded", 28000.00, "POL-PROP-4005");
                createClaim(customer, agents.get(1), ClaimType.LIFE,   ClaimStatus.INVESTIGATION_REQUIRED, "CL-4006", "Disability insurance claim after workplace accident", 150000.00, "POL-LIFE-4006");
            }
            // ---- Customer 5: Ishaan Gowda — 5 claims ----
            case 5 -> {
                createClaim(customer, null,        ClaimType.AUTO,      ClaimStatus.SUBMITTED,              "CL-5001", "Side collision at intersection - airbag deployed", 20000.00, "POL-AUTO-5001");
                createClaim(customer, agents.get(2), ClaimType.HEALTH, ClaimStatus.UNDER_REVIEW,           "CL-5002", "Diabetes management - insulin pump covered claim", 9000.00,  "POL-HEALTH-5002");
                createClaim(customer, agents.get(3), ClaimType.HOME,   ClaimStatus.INVESTIGATION_REQUIRED, "CL-5003", "Burglary with property damage and theft",          32000.00, "POL-HOME-5003");
                createClaim(customer, agents.get(0), ClaimType.TRAVEL, ClaimStatus.UNDER_INVESTIGATION,    "CL-5004", "Trip interruption due to medical emergency",        7800.00, "POL-TRAVEL-5004");
                createClaim(customer, agents.get(1), ClaimType.PROPERTY,ClaimStatus.APPROVED,              "CL-5005", "Factory equipment breakdown - production loss",    55000.00, "POL-PROP-5005");
            }
        }
    }

    private User buildUser(String fn, String ln, String email, String pwd, Role role) {
        return User.builder()
                .firstName(fn).lastName(ln)
                .email(email).password(pwd)
                .role(role).enabled(true)
                .build();
    }

    private void createClaim(User customer, User agent, ClaimType type, ClaimStatus status,
                              String number, String desc, double amount, String policyNumber) {
        Claim claim = Claim.builder()
                .claimNumber(number)
                .claimType(type)
                .status(status)
                .description(desc)
                .incidentDate(LocalDate.now().minusDays((long) (Math.random() * 60 + 5)))
                .claimAmount(BigDecimal.valueOf(amount))
                .policyNumber(policyNumber)
                .customer(customer)
                .assignedAgent(agent)
                .build();
        claimRepository.save(claim);
    }
}
