package com.jobmonitor.masterdata;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class DayOffForm {

    @NotNull(message = "Pick a date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate day;

    @Size(max = 200, message = "Keep the note under 200 characters.")
    private String note = "";

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
