package com.example.best_schedule.service;

import com.example.best_schedule.entity.Group;
import com.example.best_schedule.entity.Role;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.repository.GroupRepository;
import com.example.best_schedule.repository.HomeworkRepository;
import com.example.best_schedule.repository.LectureRepository;
import com.example.best_schedule.repository.ScheduleRepository;
import com.example.best_schedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.best_schedule.repository.SubjectGroupHoursRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final LectureRepository lectureRepository;
    private final HomeworkRepository homeworkRepository;
    private final SubjectGroupHoursRepository subjectGroupHoursRepository;

    public Group createGroup(String name,
                             Integer course,
                             String specialty,
                             String faculty) {

        Group group = Group.builder()
                .name(name)
                .course(course)
                .specialty(specialty)
                .faculty(faculty)
                .build();

        return groupRepository.save(group);
    }

    public Group addStudentToGroup(Long groupId, Long studentId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("User is not a student");
        }

        if (!group.getStudents().contains(user)) {
            group.getStudents().add(user);
        }

        return groupRepository.save(group);
    }

    public Group removeStudentFromGroup(Long groupId, Long studentId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        group.getStudents().remove(user);

        return groupRepository.save(group);
    }

    @Transactional
    public Boolean deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<User> studentsToDelete = new ArrayList<>(group.getStudents());

        List<ScheduleItem> groupSchedules = scheduleRepository.findByGroup_Id(id);

        for (ScheduleItem scheduleItem : groupSchedules) {
            homeworkRepository.deleteByScheduleItem_Id(scheduleItem.getId());
        }

        scheduleRepository.deleteByGroup_Id(id);

        lectureRepository.deleteByGroup_Id(id);

        subjectGroupHoursRepository.deleteByGroup_Id(id);

        group.getStudents().clear();
        groupRepository.save(group);

        for (User student : studentsToDelete) {
            userRepository.delete(student);
        }

        groupRepository.delete(group);

        return true;
    }

    public List<Group> findAll() {
        return groupRepository.findAll();
    }
}