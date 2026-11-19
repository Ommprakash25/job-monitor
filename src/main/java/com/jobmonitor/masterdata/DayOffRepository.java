package com.jobmonitor.masterdata;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DayOffRepository extends JpaRepository<DayOff, Long> {

    List<DayOff> findAllByOrderByDayAsc();

    boolean existsByDay(LocalDate day);
}
