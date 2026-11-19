package com.jobmonitor.masterdata;

import com.jobmonitor.mail.ConnectionCheck;
import com.jobmonitor.mail.MailGateway;
import com.jobmonitor.security.SecretCipher;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MailAccountService {

    private final MailAccountRepository accounts;
    private final SecretCipher cipher;
    private final MailGateway gateway;
    private final ProfileService profiles;

    public MailAccountService(MailAccountRepository accounts, SecretCipher cipher, MailGateway gateway,
                              ProfileService profiles) {
        this.accounts = accounts;
        this.cipher = cipher;
        this.gateway = gateway;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public Optional<MailAccount> current() {
        return accounts.findFirstByActiveTrue();
    }

    /** Saves the address, and the password if one was typed. Google shows app passwords with spaces; they're removed. */
    public void save(MailAccountForm form) {
        String address = form.getEmailAddress().strip().toLowerCase(Locale.ROOT);
        String password = form.getPassword() == null ? "" : form.getPassword().replaceAll("\\s+", "");
        MailAccount account = current().orElseGet(MailAccount::new);
        boolean changed = !address.equals(account.getEmailAddress()) || !password.isEmpty();
        account.setEmailAddress(address);
        if (!password.isEmpty()) {
            account.setSecretEncrypted(cipher.encrypt(password));
        }
        if (changed) {
            account.clearCheck();
        }
        account.setUpdatedAt(Instant.now());
        accounts.save(account);
    }

    public ConnectionCheck check() {
        MailAccount account = requireAccount();
        ConnectionCheck result = gateway.checkConnection(account.getEmailAddress(),
                cipher.decrypt(account.getSecretEncrypted()));
        account.recordCheck(result.ok(), result.message(), Instant.now());
        return result;
    }

    public void sendTestEmail() {
        MailAccount account = requireAccount();
        gateway.sendTestEmail(account.getEmailAddress(), profiles.get().getFullName(),
                cipher.decrypt(account.getSecretEncrypted()));
    }

    public void remove() {
        current().ifPresent(accounts::delete);
    }

    private MailAccount requireAccount() {
        return current().filter(MailAccount::hasSecret)
                .orElseThrow(() -> new IllegalStateException("Add the Gmail address and app password first."));
    }
}
