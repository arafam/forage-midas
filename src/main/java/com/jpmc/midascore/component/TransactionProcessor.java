package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    // RestTemplate is injected here
    public TransactionProcessor(UserRepository userRepository,
                                TransactionRepository transactionRepository,
                                RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Validate and persist transaction: must have valid sender/recipient and sufficient sender balance.
     * This method is transactional so reads/updates and the transaction record are atomic for the test.
     */
    @Transactional
    public void process(Transaction tx) {
        if (tx == null) {
            return;
        }

        long senderId = tx.getSenderId();
        long recipientId = tx.getRecipientId();
        float amount = tx.getAmount();

        // find users (note: existing UserRepository declares findById(long) that returns UserRecord)
        UserRecord sender = userRepository.findById(senderId);
        UserRecord recipient = userRepository.findById(recipientId);

        if (sender == null || recipient == null) {
            // invalid user ids -> discard
            return;
        }

        if (amount < 0) {
            // negative amounts not allowed — discard
            return;
        }

        if (sender.getBalance() < amount) {
            // insufficient funds -> discard
            return;
        }

        // Call incentive API BEFORE applying balances so we know how much to give recipient.
        float incentiveAmt = 0f;
        try {
            Incentive inc = restTemplate.postForObject("http://localhost:8080/incentive", tx, Incentive.class);
            if (inc != null) {
                incentiveAmt = inc.getAmount();
            }
        } catch (Exception ex) {
            // If the incentive service is unavailable, we continue with incentive = 0.
            logger.warn("Could not contact incentive service; proceeding with incentive=0. Error: {}", ex.getMessage());
        }

        // adjust balances:
        // - sender loses the transaction amount only
        // - recipient gains the transaction amount plus any incentive
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentiveAmt);

        // persist updated users
        userRepository.save(sender);
        userRepository.save(recipient);

        // create and save transaction record (with incentive)
        TransactionRecord record = new TransactionRecord(sender, recipient, amount);
        record.setIncentive(incentiveAmt);
        transactionRepository.save(record);
    }
}

