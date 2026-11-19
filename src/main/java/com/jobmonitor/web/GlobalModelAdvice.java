package com.jobmonitor.web;

import com.jobmonitor.mail.MailGateway;
import com.jobmonitor.mail.MailMode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalModelAdvice {

    private final MailGateway mailGateway;

    public GlobalModelAdvice(MailGateway mailGateway) {
        this.mailGateway = mailGateway;
    }

    /** Every page shows whether mail is real or fake. */
    @ModelAttribute("mailMode")
    public MailMode mailMode() {
        return mailGateway.mode();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fileTooBig(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "That file is over 10 MB. Upload a smaller one.");
        return "redirect:/master/resumes";
    }
}
