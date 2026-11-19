package com.jobmonitor.masterdata;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DayOffService {

    private final DayOffRepository daysOff;

    public DayOffService(DayOffRepository daysOff) {
        this.daysOff = daysOff;
    }

    @Transactional(readOnly = true)
    public List<DayOff> list() {
        return daysOff.findAllByOrderByDayAsc();
    }

    public void add(DayOffForm form) {
        if (daysOff.existsByDay(form.getDay())) {
            throw new IllegalArgumentException("That date is already a day off.");
        }
        String note = form.getNote() == null ? "" : form.getNote().strip();
        daysOff.save(new DayOff(form.getDay(), note));
    }

    public void delete(long id) {
        daysOff.deleteById(id);
    }
}
