package com.jobmonitor.web;

import com.jobmonitor.mail.ConnectionCheck;
import com.jobmonitor.mail.MailGatewayException;
import com.jobmonitor.mail.MailMode;
import com.jobmonitor.masterdata.MailAccount;
import com.jobmonitor.masterdata.MailAccountForm;
import com.jobmonitor.masterdata.MailAccountService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/account")
public class MailAccountController {

    private final MailAccountService accounts;

    public MailAccountController(MailAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public String show(Model model) {
        Optional<MailAccount> account = accounts.current();
        MailAccountForm form = new MailAccountForm();
        account.ifPresent(a -> form.setEmailAddress(a.getEmailAddress()));
        model.addAttribute("form", form);
        model.addAttribute("account", account.orElse(null));
        return "master/account";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") MailAccountForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        Optional<MailAccount> account = accounts.current();
        boolean hasSavedPassword = account.map(MailAccount::hasSecret).orElse(false);
        if (!hasSavedPassword && (form.getPassword() == null || form.getPassword().isBlank())) {
            result.rejectValue("password", "required", "Enter the app password.");
        }
        if (result.hasErrors()) {
            model.addAttribute("account", account.orElse(null));
            return "master/account";
        }
        accounts.save(form);
        redirect.addFlashAttribute("notice", "Gmail account saved. Use \"Test sign-in\" to check it.");
        return "redirect:/master/account";
    }

    @PostMapping("/check")
    public String check(RedirectAttributes redirect) {
        try {
            ConnectionCheck result = accounts.check();
            redirect.addFlashAttribute(result.ok() ? "notice" : "error", result.message());
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/account";
    }

    @PostMapping("/test-email")
    public String testEmail(@ModelAttribute("mailMode") MailMode mailMode, RedirectAttributes redirect) {
        try {
            accounts.sendTestEmail();
            redirect.addFlashAttribute("notice", mailMode == MailMode.FAKE
                    ? "Test email sent to the built-in fake mail server. Nothing left this computer."
                    : "Test email sent. Check the inbox of this Gmail account.");
        } catch (MailGatewayException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master/account";
    }

    @PostMapping("/remove")
    public String remove(RedirectAttributes redirect) {
        accounts.remove();
        redirect.addFlashAttribute("notice", "Gmail account removed.");
        return "redirect:/master/account";
    }
}
