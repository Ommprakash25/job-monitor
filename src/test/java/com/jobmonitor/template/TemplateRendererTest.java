package com.jobmonitor.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void fillsPlaceholders() {
        RenderedEmail email = renderer.render("Application for {role}", "Hi {name}, I'd like to join {company}.",
                new PlaceholderValues("Priya", "Infosys", "Java Developer", "Software Engineer"));
        assertThat(email.subject()).isEqualTo("Application for Java Developer");
        assertThat(email.textBody()).isEqualTo("Hi Priya, I'd like to join Infosys.");
        assertThat(email.warnings()).isEmpty();
    }

    @Test
    void usesFallbacksForBlankValues() {
        RenderedEmail email = renderer.render("Application for {role}", "Hi {name}, at {company}.",
                new PlaceholderValues("", " ", null, "Software Engineer"));
        assertThat(email.subject()).isEqualTo("Application for Software Engineer");
        assertThat(email.textBody()).isEqualTo("Hi Hiring Team, at your company.");
    }

    @Test
    void boldIsHtmlInOneVersionAndPlainInTheOther() {
        RenderedEmail email = renderer.render("s", "I have **4 years** of Java.", values());
        assertThat(email.htmlBody()).contains("<strong>4 years</strong>");
        assertThat(email.textBody()).isEqualTo("I have 4 years of Java.");
    }

    @Test
    void linksBecomeClickableWithoutTrailingPunctuation() {
        RenderedEmail email = renderer.render("s", "See https://github.com/me. Also www.example.com", values());
        assertThat(email.htmlBody())
                .contains("<a href=\"https://github.com/me\">https://github.com/me</a>.")
                .contains("<a href=\"https://www.example.com\">www.example.com</a>");
    }

    @Test
    void escapesHtmlTypedIntoTheTemplate() {
        RenderedEmail email = renderer.render("s", "<script>alert(1)</script>", values());
        assertThat(email.htmlBody()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void splitsParagraphsAndLines() {
        RenderedEmail email = renderer.render("s", "Hi,\n\nLine one\nLine two", values());
        assertThat(email.htmlBody()).isEqualTo("<div dir=\"ltr\"><p>Hi,</p><p>Line one<br>Line two</p></div>");
    }

    @Test
    void warnsAboutUnknownPlaceholdersAndBrackets() {
        RenderedEmail email = renderer.render("{role} [X] years", "Hi {firstname}, [notice period]", values());
        assertThat(email.warnings()).hasSize(2);
        assertThat(email.warnings().get(0)).contains("{firstname}");
        assertThat(email.warnings().get(1)).contains("[X]").contains("[notice period]");
        assertThat(email.textBody()).contains("{firstname}");
    }

    private static PlaceholderValues values() {
        return new PlaceholderValues("Priya", "Infosys", "Developer", "Software Engineer");
    }
}
