package com.jobmonitor.sending;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SendingPureLogicTest {

    @Test
    void guessesFirstNamesFromAddresses() {
        assertThat(NameGuesser.guess("priya.s@example.com")).isEqualTo("Priya");
        assertThat(NameGuesser.guess("rkumar@example.com")).isEqualTo("Rkumar");
        assertThat(NameGuesser.guess("hr_team@example.com")).isEmpty();
    }

    @Test
    void namesCompaniesFromDomains() {
        assertThat(RecruiterService.companyName("infosys.com")).isEqualTo("Infosys");
        assertThat(RecruiterService.companyName("ipsl.co.in")).isEqualTo("Ipsl");
        assertThat(RecruiterService.companyName("mail.csm.tech")).isEqualTo("Csm");
    }

    @Test
    void parsesOnePerLineWithOptionalName() {
        RecruiterService.Parsed parsed = RecruiterService.parse(
                "Priya@Infosys.com, Priya Sharma\n\nnot-an-address\nrahul.k@example.com\trahul");

        assertThat(parsed.valid()).containsExactly(
                new RecruiterService.Entry("priya@infosys.com", "Priya Sharma"),
                new RecruiterService.Entry("rahul.k@example.com", "rahul"));
        assertThat(parsed.invalid()).containsExactly("not-an-address");
    }

    @Test
    void rejectsAddressesWithStrayCharactersAtTheStart() {
        RecruiterService.Parsed parsed = RecruiterService.parse("-hiring4@3ginfotech.in\n_x@example.com");
        assertThat(parsed.valid()).isEmpty();
        assertThat(parsed.invalid()).containsExactly("-hiring4@3ginfotech.in", "_x@example.com");
    }

    @Test
    void emptyInputGivesNothing() {
        RecruiterService.Parsed parsed = RecruiterService.parse(null);
        assertThat(parsed.valid()).isEmpty();
        assertThat(parsed.invalid()).isEmpty();
    }
}
