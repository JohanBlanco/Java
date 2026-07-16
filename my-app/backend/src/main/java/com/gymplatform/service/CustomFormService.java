package com.gymplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymplatform.domain.entity.CustomForm;
import com.gymplatform.domain.entity.CustomFormSubmission;
import com.gymplatform.domain.entity.FormFolder;
import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.entity.User;
import com.gymplatform.domain.enums.FormAccessType;
import com.gymplatform.domain.enums.FormFieldType;
import com.gymplatform.domain.enums.FormFolderKind;
import com.gymplatform.domain.enums.FormPurpose;
import com.gymplatform.dto.*;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.CustomFormRepository;
import com.gymplatform.repository.CustomFormSubmissionRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.repository.UserRepository;
import com.gymplatform.util.MemberRegistrationFormFactory;
import com.gymplatform.util.SecurityUtils;
import com.gymplatform.util.SlugHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomFormService {

    private final CustomFormRepository formRepository;
    private final CustomFormSubmissionRepository submissionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final FormFolderService folderService;
    private final BroadcastSettingsService broadcastSettingsService;
    private final MemberFileService memberFileService;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    public CustomFormService(
            CustomFormRepository formRepository,
            CustomFormSubmissionRepository submissionRepository,
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            FormFolderService folderService,
            BroadcastSettingsService broadcastSettingsService,
            MemberFileService memberFileService) {
        this.formRepository = formRepository;
        this.submissionRepository = submissionRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.folderService = folderService;
        this.broadcastSettingsService = broadcastSettingsService;
        this.memberFileService = memberFileService;
    }

    @Transactional
    public CustomForm ensureMemberRegistrationForm(Long organizationId) {
        return formRepository.findByOrganizationIdAndFormPurpose(organizationId, FormPurpose.MEMBER_REGISTRATION)
                .map(this::upgradeRegistrationFormIfNeeded)
                .orElseGet(() -> createMemberRegistrationForm(organizationId));
    }

    private CustomForm upgradeRegistrationFormIfNeeded(CustomForm form) {
        List<FormFieldDto> fields = readFields(form.getFieldsJson());
        boolean needsUpgrade = fields.stream()
                .anyMatch(field -> "f-firma".equals(field.id()) && field.type() == FormFieldType.TEXT)
                || fields.stream().noneMatch(field -> field.type() == FormFieldType.SIGNATURE);
        if (!needsUpgrade) {
            return form;
        }
        form.setFieldsJson(writeFields(MemberRegistrationFormFactory.defaultFields()));
        form.setUpdatedAt(Instant.now());
        return formRepository.save(form);
    }

    public Optional<String> buildRegistrationFormWhatsappUrl(Long organizationId, User user) {
        CustomForm form = ensureMemberRegistrationForm(organizationId);
        Organization org = form.getOrganization();
        String url = buildPublicUrl(org.getSlug(), form.getSlug(), user.getId());
        return broadcastSettingsService.buildRegistrationFormWhatsappUrl(
                organizationId,
                user.getWhatsappPhone(),
                user.getFirstName(),
                form.getTitle(),
                url);
    }

    public WhatsappOutboundResponse resendRegistrationFormViaWhatsApp(Long organizationId, User user) {
        CustomForm form = ensureMemberRegistrationForm(organizationId);
        Organization org = form.getOrganization();
        String url = buildPublicUrl(org.getSlug(), form.getSlug(), user.getId());
        String message = broadcastSettingsService.previewRegistrationFormMessage(
                user.getFirstName(),
                form.getTitle(),
                url);
        String whatsappUrl = broadcastSettingsService.requireRegistrationFormWhatsappUrl(
                organizationId,
                user.getWhatsappPhone(),
                user.getFirstName(),
                form.getTitle(),
                url);
        return new WhatsappOutboundResponse(whatsappUrl, message);
    }

    public void sendRegistrationFormViaWhatsApp(Long organizationId, User user) {
        buildRegistrationFormWhatsappUrl(organizationId, user);
    }

    private CustomForm createMemberRegistrationForm(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        CustomForm form = new CustomForm();
        form.setOrganization(org);
        form.setTitle(MemberRegistrationFormFactory.TITLE);
        form.setSlug(MemberRegistrationFormFactory.SLUG);
        form.setDescription(MemberRegistrationFormFactory.DESCRIPTION);
        form.setAccessType(FormAccessType.PUBLIC);
        form.setFormPurpose(FormPurpose.MEMBER_REGISTRATION);
        form.setActive(true);
        form.setFieldsJson(writeFields(MemberRegistrationFormFactory.defaultFields()));
        form = formRepository.save(form);

        FormFolder responseFolder = folderService.createAutoResponseFolder(form);
        form.setResponseFolder(responseFolder);
        return formRepository.save(form);
    }

    public List<CustomFormResponse> listForms(Long organizationId, Long templateFolderId) {
        requireConfigRole();
        ensureMemberRegistrationForm(organizationId);
        List<CustomForm> forms = templateFolderId == null
                ? formRepository.findByOrganizationIdOrderByTitleAsc(organizationId)
                : templateFolderId == -1L
                ? formRepository.findWithoutTemplateFolder(organizationId)
                : formRepository.findByOrganizationIdAndTemplateFolderIdOrderByTitleAsc(
                        organizationId, templateFolderId);
        return forms.stream().map(this::toResponse).toList();
    }

    public CustomFormResponse getForm(Long organizationId, Long formId) {
        requireConfigRole();
        return toResponse(requireForm(organizationId, formId));
    }

    @Transactional
    public CustomFormResponse createForm(Long organizationId, CustomFormRequest request) {
        requireConfigRole();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        String slug = resolveSlug(organizationId, request.slug(), request.title(), null);
        List<FormFieldDto> fields = normalizeFields(request.fields());

        CustomForm form = new CustomForm();
        form.setOrganization(org);
        form.setTitle(request.title().trim());
        form.setSlug(slug);
        form.setDescription(trimToNull(request.description()));
        form.setAccessType(request.accessType());
        form.setActive(request.active());
        form.setFieldsJson(writeFields(fields));
        form = formRepository.save(form);

        FormFolder responseFolder = resolveResponseFolder(organizationId, form, request.responseFolderId(), true);
        form.setResponseFolder(responseFolder);
        if (request.templateFolderId() != null) {
            form.setTemplateFolder(folderService.requireFolder(
                    organizationId, request.templateFolderId(), FormFolderKind.TEMPLATE));
        }

        return toResponse(formRepository.save(form));
    }

    @Transactional
    public CustomFormResponse updateForm(Long organizationId, Long formId, CustomFormRequest request) {
        requireConfigRole();
        CustomForm form = requireForm(organizationId, formId);
        String slug = form.getFormPurpose() == FormPurpose.MEMBER_REGISTRATION
                ? MemberRegistrationFormFactory.SLUG
                : resolveSlug(organizationId, request.slug(), request.title(), form.getId());
        List<FormFieldDto> fields = normalizeFields(request.fields());

        form.setTitle(request.title().trim());
        form.setSlug(slug);
        form.setDescription(trimToNull(request.description()));
        form.setAccessType(request.accessType());
        form.setActive(request.active());
        form.setFieldsJson(writeFields(fields));
        if (request.templateFolderId() != null) {
            form.setTemplateFolder(folderService.requireFolder(
                    organizationId, request.templateFolderId(), FormFolderKind.TEMPLATE));
        } else {
            form.setTemplateFolder(null);
        }
        if (request.responseFolderId() != null) {
            form.setResponseFolder(folderService.requireFolder(
                    organizationId, request.responseFolderId(), FormFolderKind.RESPONSE));
        } else if (form.getResponseFolder() == null) {
            form.setResponseFolder(resolveResponseFolder(organizationId, form, null, true));
        }
        form.setUpdatedAt(Instant.now());

        return toResponse(formRepository.save(form));
    }

    @Transactional
    public void deleteForm(Long organizationId, Long formId) {
        requireConfigRole();
        CustomForm form = requireForm(organizationId, formId);
        if (form.getFormPurpose() == FormPurpose.MEMBER_REGISTRATION) {
            throw new BusinessException("El formulario de registro del miembro no se puede eliminar");
        }
        formRepository.delete(form);
    }

    public PublicFormResponse getPublicForm(String organizationSlug, String formSlug, boolean authenticated) {
        CustomForm form = formRepository.findByOrganization_SlugAndSlug(organizationSlug, formSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));

        if (!form.isActive()) {
            throw new BusinessException("Este formulario no está disponible");
        }

        Organization org = form.getOrganization();
        if (!org.isActive()) {
            throw new BusinessException("El gimnasio no está activo");
        }

        boolean requiresAuth = form.getAccessType() == FormAccessType.AUTHENTICATED;
        List<FormFieldDto> fields = List.of();
        if (!requiresAuth || authenticated) {
            if (requiresAuth) {
                validateSameOrganization(org.getId());
            }
            fields = readFields(form.getFieldsJson());
        }

        return new PublicFormResponse(
                form.getId(),
                form.getTitle(),
                form.getSlug(),
                form.getDescription(),
                form.getAccessType(),
                requiresAuth && !authenticated,
                org.getName(),
                org.getSlug(),
                fields
        );
    }

    @Transactional
    public FormSubmissionResponse submitPublicForm(String organizationSlug, String formSlug, FormSubmissionRequest request) {
        CustomForm form = formRepository.findByOrganization_SlugAndSlug(organizationSlug, formSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
        if (!form.isActive()) {
            throw new BusinessException("Este formulario no está disponible");
        }
        if (form.getAccessType() != FormAccessType.PUBLIC) {
            throw new BusinessException("Este formulario requiere iniciar sesión");
        }
        User linkedMember = memberFileService.resolveLinkedMember(form.getOrganization().getId(), request.memberUserId());
        return saveSubmission(form, linkedMember, request.answers());
    }

    @Transactional
    public FormSubmissionResponse submitAuthenticatedForm(Long organizationId, Long formId, FormSubmissionRequest request) {
        CustomForm form = requireForm(organizationId, formId);
        if (!form.isActive()) {
            throw new BusinessException("Este formulario no está disponible");
        }
        User user = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return saveSubmission(form, user, request.answers());
    }

    public String resolveFormLink(Long organizationId, String formSlug) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
        CustomForm form = formRepository.findByOrganizationIdAndSlug(organizationId, formSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
        return buildPublicUrl(org.getSlug(), form.getSlug());
    }

    public List<FormSubmissionDetailResponse> listSubmissions(Long organizationId, Long responseFolderId) {
        requireConfigRole();
        folderService.requireFolder(organizationId, responseFolderId, FormFolderKind.RESPONSE);
        return submissionRepository.findByResponseFolderIdOrderByCreatedAtDesc(responseFolderId).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    private FormSubmissionDetailResponse toSubmissionResponse(CustomFormSubmission submission) {
        User submitter = submission.getSubmittedBy();
        FormFolder folder = submission.getResponseFolder();
        return new FormSubmissionDetailResponse(
                submission.getId(),
                submission.getForm().getId(),
                submission.getForm().getTitle(),
                folder != null ? folder.getId() : null,
                folder != null ? folder.getName() : null,
                submitter != null ? submitter.getFirstName() + " " + submitter.getLastName() : null,
                readAnswerMap(submission.getAnswersJson()),
                submission.getCreatedAt(),
                submission.getImportedAt()
        );
    }

    private FormFolder resolveResponseFolder(
            Long organizationId,
            CustomForm form,
            Long responseFolderId,
            boolean createAutoIfMissing) {
        if (responseFolderId != null) {
            return folderService.requireFolder(organizationId, responseFolderId, FormFolderKind.RESPONSE);
        }
        if (form.getResponseFolder() != null) {
            return form.getResponseFolder();
        }
        if (!createAutoIfMissing) {
            throw new BusinessException("Selecciona una carpeta de respuestas");
        }
        return folderService.createAutoResponseFolder(form);
    }

    private FormFolder ensureResponseFolder(CustomForm form) {
        if (form.getResponseFolder() != null) {
            return form.getResponseFolder();
        }
        FormFolder folder = folderService.createAutoResponseFolder(form);
        form.setResponseFolder(folder);
        formRepository.save(form);
        return folder;
    }

    private FormSubmissionResponse saveSubmission(CustomForm form, User user, Map<String, Object> answers) {
        List<FormFieldDto> fields = readFields(form.getFieldsJson());
        Map<String, Object> normalizedAnswers = validateAnswers(fields, answers);

        CustomFormSubmission submission = new CustomFormSubmission();
        submission.setForm(form);
        submission.setSubmittedBy(user);
        submission.setResponseFolder(ensureResponseFolder(form));
        submission.setAnswersJson(writeAnswers(normalizedAnswers));
        submission = submissionRepository.save(submission);
        return new FormSubmissionResponse(submission.getId(), submission.getCreatedAt());
    }

    private Map<String, Object> validateAnswers(List<FormFieldDto> fields, Map<String, Object> answers) {
        if (answers == null) {
            throw new BusinessException("Las respuestas son obligatorias");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (FormFieldDto field : fields) {
            if (field.type() == FormFieldType.HEADING) {
                continue;
            }
            if (!isFieldVisible(field, answers)) {
                continue;
            }
            Object value = answers.get(field.id());
            boolean blank = isBlankAnswer(field, value);
            if (field.required() && blank) {
                throw new BusinessException("Completa el campo: " + field.label());
            }
            if (!blank) {
                normalized.put(field.id(), value);
            }
        }
        return normalized;
    }

    private boolean isFieldVisible(FormFieldDto field, Map<String, Object> answers) {
        if (field.visibilityFieldId() == null || field.visibilityFieldId().isBlank()) {
            return true;
        }
        Object answer = answers.get(field.visibilityFieldId());
        String expected = field.visibilityValue() != null ? field.visibilityValue() : "";
        if (answer == null) {
            return false;
        }
        if (answer instanceof Boolean bool) {
            return String.valueOf(bool).equals(expected);
        }
        return String.valueOf(answer).equals(expected);
    }

    private boolean isBlankAnswer(FormFieldDto field, Object value) {
        if (value == null) {
            return true;
        }
        if (field.type() == FormFieldType.SIGNATURE) {
            String text = String.valueOf(value);
            return text.isBlank() || !text.startsWith("data:image");
        }
        return String.valueOf(value).isBlank();
    }

    private List<FormFieldDto> normalizeFields(List<FormFieldDto> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException("Agrega al menos un campo al formulario");
        }
        List<FormFieldDto> normalized = new ArrayList<>();
        for (FormFieldDto field : fields) {
            if (field.type() == null) {
                throw new BusinessException("Cada campo debe tener un tipo");
            }
            String label = field.label() != null ? field.label().trim() : "";
            if (field.type() != FormFieldType.HEADING && label.isBlank()) {
                throw new BusinessException("Cada campo debe tener una etiqueta");
            }
            if (field.type() == FormFieldType.HEADING && label.isBlank()) {
                label = "Sección";
            }
            String id = field.id() != null && !field.id().isBlank() ? field.id().trim() : UUID.randomUUID().toString();
            List<String> options = field.options() != null
                    ? field.options().stream().map(String::trim).filter(s -> !s.isBlank()).toList()
                    : List.of();
            if ((field.type() == FormFieldType.SELECT || field.type() == FormFieldType.RADIO)
                    && options.size() < 2) {
                throw new BusinessException("El campo \"" + label + "\" necesita al menos 2 opciones");
            }
            normalized.add(new FormFieldDto(
                    id,
                    field.type(),
                    label,
                    trimToNull(field.placeholder()),
                    trimToNull(field.helpText()),
                    field.required(),
                    options,
                    trimToNull(field.visibilityFieldId()),
                    trimToNull(field.visibilityValue())
            ));
        }
        return normalized;
    }

    private String resolveSlug(Long organizationId, String requestedSlug, String title, Long currentFormId) {
        String base = requestedSlug != null && !requestedSlug.isBlank()
                ? SlugHelper.slugify(requestedSlug)
                : SlugHelper.slugify(title);
        String candidate = base;
        int suffix = 2;
        while (isSlugTaken(organizationId, candidate, currentFormId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean isSlugTaken(Long organizationId, String slug, Long currentFormId) {
        return formRepository.findByOrganizationIdAndSlug(organizationId, slug)
                .filter(existing -> currentFormId == null || !existing.getId().equals(currentFormId))
                .isPresent();
    }

    private CustomForm requireForm(Long organizationId, Long formId) {
        return formRepository.findByIdAndOrganizationId(formId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Formulario no encontrado"));
    }

    private CustomFormResponse toResponse(CustomForm form) {
        Organization org = form.getOrganization();
        FormFolder templateFolder = form.getTemplateFolder();
        FormFolder responseFolder = form.getResponseFolder() != null
                ? form.getResponseFolder()
                : null;
        long submissionCount = responseFolder != null
                ? submissionRepository.countByResponseFolderId(responseFolder.getId())
                : 0;
        return new CustomFormResponse(
                form.getId(),
                form.getTitle(),
                form.getSlug(),
                form.getDescription(),
                form.getAccessType(),
                form.getFormPurpose(),
                form.getFormPurpose() == FormPurpose.MEMBER_REGISTRATION,
                form.isActive(),
                readFields(form.getFieldsJson()),
                buildPublicUrl(org.getSlug(), form.getSlug()),
                templateFolder != null ? templateFolder.getId() : null,
                templateFolder != null ? templateFolder.getName() : null,
                responseFolder != null ? responseFolder.getId() : null,
                responseFolder != null ? responseFolder.getName() : null,
                submissionCount,
                form.getCreatedAt(),
                form.getUpdatedAt()
        );
    }

    private String buildPublicUrl(String organizationSlug, String formSlug) {
        return buildPublicUrl(organizationSlug, formSlug, null);
    }

    private String buildPublicUrl(String organizationSlug, String formSlug, Long memberUserId) {
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String url = base + "/f/" + organizationSlug + "/" + formSlug;
        if (memberUserId != null) {
            url += "?m=" + memberUserId;
        }
        return url;
    }

    private List<FormFieldDto> readFields(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<FormFieldDto>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudieron leer los campos del formulario");
        }
    }

    private String writeFields(List<FormFieldDto> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudieron guardar los campos del formulario");
        }
    }

    private String writeAnswers(Map<String, Object> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudieron guardar las respuestas");
        }
    }

    private Map<String, Object> readAnswerMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("No se pudieron leer las respuestas");
        }
    }

    private void validateSameOrganization(Long organizationId) {
        Long currentOrgId = SecurityUtils.requireOrganizationId();
        if (!organizationId.equals(currentOrgId)) {
            throw new BusinessException("No tienes acceso a este formulario");
        }
    }

    private void requireConfigRole() {
        var user = SecurityUtils.currentUser();
        if (!user.hasRole("GYM_OWNER") && !user.hasRole("RECEPTIONIST")) {
            throw new BusinessException("No tienes permiso para gestionar formularios");
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
