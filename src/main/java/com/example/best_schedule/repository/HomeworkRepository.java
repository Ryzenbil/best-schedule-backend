package com.example.best_schedule.repository;

import com.example.best_schedule.entity.Homework;
import com.example.best_schedule.entity.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    void deleteByScheduleItem_Id(Long scheduleItemId);
    List<Homework> findByScheduleItem(ScheduleItem scheduleItem);
    void deleteByScheduleItemId(Long scheduleItemId);
}