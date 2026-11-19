package com.jobmonitor.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** A date when nothing is sent, such as a public holiday. */
@Entity
@Table(name = "day_off")
public class DayOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate day;

    @Column(nullable = false, length = 200)
    private String note = "";

    protected DayOff() {
    }

    public DayOff(LocalDate day, String note) {
        this.day = day;
        this.note = note == null ? "" : note;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDay() {
        return day;
    }

    public String getNote() {
        return note;
    }
}
