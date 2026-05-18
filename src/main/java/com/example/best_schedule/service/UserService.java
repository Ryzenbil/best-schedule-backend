package com.example.best_schedule.service;

import com.example.best_schedule.dto.AuthResponse;
import com.example.best_schedule.dto.RegisterInput;
import com.example.best_schedule.entity.Group;
import com.example.best_schedule.entity.Role;
import com.example.best_schedule.entity.ScheduleItem;
import com.example.best_schedule.entity.User;
import com.example.best_schedule.exception.EmailAlreadyExistsException;
import com.example.best_schedule.exception.InvalidCredentialsException;
import com.example.best_schedule.repository.GroupRepository;
import com.example.best_schedule.repository.HomeworkRepository;
import com.example.best_schedule.repository.LectureRepository;
import com.example.best_schedule.repository.ScheduleRepository;
import com.example.best_schedule.repository.UserRepository;
import com.example.best_schedule.security.JwtUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final LectureRepository lectureRepository;
    private final HomeworkRepository homeworkRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User getCurrentUser() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public User register(RegisterInput input) {

        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = User.builder()
                .name(input.getName())
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .role(Role.valueOf(input.getRole()))
                .build();

        return userRepository.save(user);
    }

    public AuthResponse login(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());

        return new AuthResponse(token, user);
    }

    public User updateUser(Long id, String name, String email, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (name != null) user.setName(name);
        if (email != null) user.setEmail(email);
        if (role != null) user.setRole(Role.valueOf(role));

        return userRepository.save(user);
    }

    @Transactional
    public Boolean deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Group> groups = groupRepository.findAll();

        for (Group group : groups) {
            group.getStudents().remove(user);
        }

        groupRepository.saveAll(groups);

        if (user.getRole() == Role.TEACHER) {
            List<ScheduleItem> teacherSchedules = scheduleRepository.findByTeacherId(id);

            for (ScheduleItem scheduleItem : teacherSchedules) {
                homeworkRepository.deleteByScheduleItemId(scheduleItem.getId());
            }

            scheduleRepository.deleteByTeacherId(id);
            lectureRepository.deleteByTeacherId(id);
        }

        userRepository.delete(user);

        return true;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }
}