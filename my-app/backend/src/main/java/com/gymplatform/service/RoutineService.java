package com.gymplatform.service;

import com.gymplatform.domain.entity.*;
import com.gymplatform.domain.enums.RoutineRequestStatus;
import com.gymplatform.dto.*;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final RoutineTemplateRepository templateRepository;
    private final RoutineRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public RoutineService(RoutineRepository routineRepository, RoutineTemplateRepository templateRepository,
                          RoutineRequestRepository requestRepository, UserRepository userRepository,
                          OrganizationRepository organizationRepository) {
        this.routineRepository = routineRepository;
        this.templateRepository = templateRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public RoutineTemplateResponse createTemplate(Long organizationId, Long instructorId, RoutineTemplateRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));

        RoutineTemplate template = new RoutineTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        template.setGoal(request.goal());
        template.setInstructor(instructor);
        template.setOrganization(org);
        addExercisesToTemplate(template, request.exercises());

        return toTemplateResponse(templateRepository.save(template));
    }

    public List<RoutineTemplateResponse> findTemplates(Long organizationId) {
        return templateRepository.findByOrganizationIdAndActiveTrue(organizationId)
                .stream().map(this::toTemplateResponse).toList();
    }

    @Transactional
    public RoutineResponse createRoutine(Long organizationId, Long instructorId, CreateRoutineRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
        User member = userRepository.findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));

        Routine routine = new Routine();
        routine.setName(request.name());
        routine.setDescription(request.description());
        routine.setNotes(request.notes());
        routine.setMember(member);
        routine.setInstructor(instructor);
        routine.setOrganization(org);
        routine.setTemporary(request.temporary());

        if (request.templateId() != null) {
            RoutineTemplate template = templateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
            routine.setTemplate(template);
            if (request.exercises() == null || request.exercises().isEmpty()) {
                copyExercisesFromTemplate(routine, template);
            } else {
                addExercisesToRoutine(routine, request.exercises());
            }
        } else {
            addExercisesToRoutine(routine, request.exercises());
        }

        return toRoutineResponse(routineRepository.save(routine));
    }

    @Transactional
    public List<RoutineResponse> assignTemplate(Long organizationId, Long instructorId, AssignTemplateRequest request) {
        RoutineTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));

        List<RoutineResponse> results = new ArrayList<>();
        for (Long memberId : request.memberIds()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado: " + memberId));

            Routine routine = new Routine();
            routine.setName(template.getName());
            routine.setDescription(template.getDescription());
            routine.setMember(member);
            routine.setInstructor(instructor);
            routine.setOrganization(org);
            routine.setTemplate(template);
            routine.setTemporary(false);
            copyExercisesFromTemplate(routine, template);

            results.add(toRoutineResponse(routineRepository.save(routine)));
        }
        return results;
    }

    public List<RoutineResponse> findByMember(Long memberId) {
        return routineRepository.findByMemberIdAndActiveTrue(memberId)
                .stream().map(this::toRoutineResponse).toList();
    }

    @Transactional
    public RoutineRequestResponse createRequest(Long organizationId, Long memberId, RoutineRequestCreate request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Miembro no encontrado"));

        RoutineRequest routineRequest = new RoutineRequest();
        routineRequest.setMember(member);
        routineRequest.setOrganization(org);
        routineRequest.setDescription(request.description());
        routineRequest.setGoals(request.goals());

        return toRequestResponse(requestRepository.save(routineRequest));
    }

    public List<RoutineRequestResponse> findRequests(Long organizationId) {
        return requestRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream().map(this::toRequestResponse).toList();
    }

    @Transactional
    public RoutineRequestResponse updateRequestStatus(Long requestId, RoutineRequestStatus status, Long instructorId) {
        RoutineRequest routineRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        routineRequest.setStatus(status);
        routineRequest.setUpdatedAt(Instant.now());
        if (instructorId != null) {
            User instructor = userRepository.findById(instructorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor no encontrado"));
            routineRequest.setAssignedInstructor(instructor);
        }
        return toRequestResponse(requestRepository.save(routineRequest));
    }

    private void addExercisesToTemplate(RoutineTemplate template, List<RoutineExerciseRequest> exercises) {
        if (exercises == null) return;
        for (RoutineExerciseRequest ex : exercises) {
            RoutineExercise exercise = mapExercise(ex);
            exercise.setTemplate(template);
            template.getExercises().add(exercise);
        }
    }

    private void addExercisesToRoutine(Routine routine, List<RoutineExerciseRequest> exercises) {
        if (exercises == null) return;
        for (RoutineExerciseRequest ex : exercises) {
            RoutineExercise exercise = mapExercise(ex);
            exercise.setRoutine(routine);
            routine.getExercises().add(exercise);
        }
    }

    private void copyExercisesFromTemplate(Routine routine, RoutineTemplate template) {
        int index = 0;
        for (RoutineExercise source : template.getExercises()) {
            RoutineExercise exercise = new RoutineExercise();
            exercise.setExerciseName(source.getExerciseName());
            exercise.setSets(source.getSets());
            exercise.setReps(source.getReps());
            exercise.setWeight(source.getWeight());
            exercise.setDurationSeconds(source.getDurationSeconds());
            exercise.setNotes(source.getNotes());
            exercise.setOrderIndex(index++);
            exercise.setRoutine(routine);
            routine.getExercises().add(exercise);
        }
    }

    private RoutineExercise mapExercise(RoutineExerciseRequest ex) {
        RoutineExercise exercise = new RoutineExercise();
        exercise.setExerciseName(ex.exerciseName());
        exercise.setSets(ex.sets());
        exercise.setReps(ex.reps());
        exercise.setWeight(ex.weight());
        exercise.setDurationSeconds(ex.durationSeconds());
        exercise.setNotes(ex.notes());
        exercise.setOrderIndex(ex.orderIndex());
        return exercise;
    }

    private RoutineTemplateResponse toTemplateResponse(RoutineTemplate template) {
        List<RoutineExerciseResponse> exercises = template.getExercises().stream()
                .map(this::toExerciseResponse).toList();
        return new RoutineTemplateResponse(
                template.getId(), template.getName(), template.getDescription(),
                template.getGoal(), template.getInstructor().getId(), exercises
        );
    }

    private RoutineResponse toRoutineResponse(Routine routine) {
        List<RoutineExerciseResponse> exercises = routine.getExercises().stream()
                .map(this::toExerciseResponse).toList();
        return new RoutineResponse(
                routine.getId(), routine.getName(), routine.getDescription(), routine.getNotes(),
                routine.getMember().getId(),
                routine.getMember().getFirstName() + " " + routine.getMember().getLastName(),
                routine.getInstructor().getId(),
                routine.getInstructor().getFirstName() + " " + routine.getInstructor().getLastName(),
                routine.getTemplate() != null ? routine.getTemplate().getId() : null,
                routine.isTemporary(), exercises
        );
    }

    private RoutineRequestResponse toRequestResponse(RoutineRequest request) {
        String instructorName = request.getAssignedInstructor() != null
                ? request.getAssignedInstructor().getFirstName() + " " + request.getAssignedInstructor().getLastName()
                : null;
        return new RoutineRequestResponse(
                request.getId(),
                request.getMember().getId(),
                request.getMember().getFirstName() + " " + request.getMember().getLastName(),
                request.getDescription(),
                request.getGoals(),
                request.getStatus().name(),
                request.getAssignedInstructor() != null ? request.getAssignedInstructor().getId() : null,
                instructorName
        );
    }

    private RoutineExerciseResponse toExerciseResponse(RoutineExercise ex) {
        return new RoutineExerciseResponse(
                ex.getId(), ex.getExerciseName(), ex.getSets(), ex.getReps(),
                ex.getWeight(), ex.getDurationSeconds(), ex.getNotes(), ex.getOrderIndex()
        );
    }
}
