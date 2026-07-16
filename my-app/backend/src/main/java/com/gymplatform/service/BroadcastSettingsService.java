package com.gymplatform.service;

import com.gymplatform.domain.entity.BroadcastChannelSettings;
import com.gymplatform.domain.entity.BroadcastMessageTemplate;
import com.gymplatform.domain.entity.Organization;
import com.gymplatform.domain.enums.BroadcastChannel;
import com.gymplatform.domain.enums.BroadcastTemplatePurpose;
import com.gymplatform.dto.BroadcastChannelSettingsRequest;
import com.gymplatform.dto.BroadcastChannelSettingsResponse;
import com.gymplatform.dto.BroadcastMessageTemplateRequest;
import com.gymplatform.dto.BroadcastMessageTemplateResponse;
import com.gymplatform.exception.BusinessException;
import com.gymplatform.exception.ResourceNotFoundException;
import com.gymplatform.repository.BroadcastChannelSettingsRepository;
import com.gymplatform.repository.BroadcastMessageTemplateRepository;
import com.gymplatform.repository.OrganizationRepository;
import com.gymplatform.util.SecurityUtils;
import com.gymplatform.util.WhatsAppLinkHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class BroadcastSettingsService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    private final BroadcastChannelSettingsRepository channelSettingsRepository;
    private final BroadcastMessageTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;

    public BroadcastSettingsService(
            BroadcastChannelSettingsRepository channelSettingsRepository,
            BroadcastMessageTemplateRepository templateRepository,
            OrganizationRepository organizationRepository) {
        this.channelSettingsRepository = channelSettingsRepository;
        this.templateRepository = templateRepository;
        this.organizationRepository = organizationRepository;
    }

    public BroadcastChannelSettingsResponse getChannelSettings(Long organizationId, BroadcastChannel channel) {
        requireConfigRole();
        return toSettingsResponse(getOrCreateSettings(organizationId, channel));
    }

    @Transactional
    public BroadcastChannelSettingsResponse updateChannelSettings(
            Long organizationId,
            BroadcastChannel channel,
            BroadcastChannelSettingsRequest request) {
        requireConfigRole();
        BroadcastChannelSettings settings = getOrCreateSettings(organizationId, channel);

        if (Boolean.TRUE.equals(request.enabled())) {
            if (!Boolean.TRUE.equals(request.whatsappWebSessionConfirmed())) {
                throw new BusinessException(
                        "Debes iniciar sesión en WhatsApp Web y confirmarlo antes de activar los envíos automáticos");
            }
            String phone = normalizePhone(request.senderPhone());
            if (phone == null || phone.isBlank()) {
                throw new BusinessException("Indica el número de WhatsApp para activar los envíos automáticos");
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException("El número debe incluir código de país, por ejemplo +5215512345678");
            }
            settings.setSenderPhone(phone);
            settings.setWhatsappWebSessionConfirmed(true);
            settings.setEnabled(true);
        } else {
            settings.setEnabled(false);
            settings.setWhatsappWebSessionConfirmed(false);
            if (request.senderPhone() != null && !request.senderPhone().isBlank()) {
                settings.setSenderPhone(normalizePhone(request.senderPhone()));
            }
        }

        settings.setUpdatedAt(Instant.now());
        return toSettingsResponse(channelSettingsRepository.save(settings));
    }

    public List<BroadcastMessageTemplateResponse> listTemplates(
            Long organizationId,
            BroadcastChannel channel,
            BroadcastTemplatePurpose purpose) {
        requireConfigRole();
        List<BroadcastMessageTemplate> templates = purpose != null
                ? templateRepository.findByOrganizationIdAndChannelAndPurposeOrderByNameAsc(
                        organizationId, channel, purpose)
                : templateRepository.findByOrganizationIdAndChannelOrderByNameAsc(organizationId, channel);
        return templates.stream().map(this::toTemplateResponse).toList();
    }

    @Transactional
    public BroadcastMessageTemplateResponse createTemplate(
            Long organizationId,
            BroadcastChannel channel,
            BroadcastMessageTemplateRequest request) {
        requireConfigRole();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));

        BroadcastMessageTemplate template = new BroadcastMessageTemplate();
        template.setOrganization(org);
        template.setChannel(channel);
        template.setName(request.name().trim());
        template.setBody(request.body().trim());
        template.setPurpose(resolvePurpose(request.purpose()));
        return toTemplateResponse(templateRepository.save(template));
    }

    @Transactional
    public BroadcastMessageTemplateResponse updateTemplate(
            Long organizationId,
            Long templateId,
            BroadcastMessageTemplateRequest request) {
        requireConfigRole();
        BroadcastMessageTemplate template = templateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        template.setName(request.name().trim());
        template.setBody(request.body().trim());
        template.setPurpose(resolvePurpose(request.purpose()));
        template.setUpdatedAt(Instant.now());
        return toTemplateResponse(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Long organizationId, Long templateId) {
        requireConfigRole();
        BroadcastMessageTemplate template = templateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
        templateRepository.delete(template);
    }

    @Transactional
    public String sendWelcomeMessage(Long organizationId, String recipientPhone, Long templateId) {
        BroadcastMessageTemplate template = templateRepository.findByIdAndOrganizationId(templateId, organizationId)
                .orElseThrow(() -> new BusinessException("Plantilla de bienvenida no encontrada"));
        if (template.getPurpose() != BroadcastTemplatePurpose.WELCOME) {
            throw new BusinessException("La plantilla seleccionada no es de bienvenida");
        }
        return requireTemplateWhatsappUrl(organizationId, recipientPhone, template.getBody());
    }

    public String requireTemplateWhatsappUrl(
            Long organizationId,
            String recipientPhone,
            String messageBody) {
        BroadcastChannelSettings settings = getOrCreateSettings(organizationId, BroadcastChannel.WHATSAPP);
        if (!settings.isEnabled()) {
            throw new BusinessException("Activa WhatsApp en Configuración → Mensajes de difusión para enviar mensajes");
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new BusinessException("El usuario no tiene número de WhatsApp");
        }
        if (messageBody == null || messageBody.isBlank()) {
            throw new BusinessException("El mensaje no puede estar vacío");
        }
        return WhatsAppLinkHelper.buildChatUrl(recipientPhone, messageBody.trim());
    }

    public Optional<String> buildRegistrationFormWhatsappUrl(
            Long organizationId,
            String recipientPhone,
            String recipientFirstName,
            String formTitle,
            String formUrl) {
        BroadcastChannelSettings settings = getOrCreateSettings(organizationId, BroadcastChannel.WHATSAPP);
        if (!settings.isEnabled()) {
            return Optional.empty();
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(buildRegistrationFormWhatsappUrl(
                recipientPhone,
                recipientFirstName,
                formTitle,
                formUrl));
    }

    public String requireRegistrationFormWhatsappUrl(
            Long organizationId,
            String recipientPhone,
            String recipientFirstName,
            String formTitle,
            String formUrl) {
        BroadcastChannelSettings settings = getOrCreateSettings(organizationId, BroadcastChannel.WHATSAPP);
        if (!settings.isEnabled()) {
            throw new BusinessException(
                    "Activa WhatsApp en Configuración → Mensajes de difusión para enviar el formulario de registro");
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new BusinessException("El usuario no tiene número de WhatsApp");
        }
        return buildRegistrationFormWhatsappUrl(
                recipientPhone,
                recipientFirstName,
                formTitle,
                formUrl);
    }

    public String previewRegistrationFormMessage(
            String recipientFirstName,
            String formTitle,
            String formUrl) {
        return buildRegistrationFormMessage(recipientFirstName, formTitle, formUrl);
    }

    private String buildRegistrationFormWhatsappUrl(
            String recipientPhone,
            String recipientFirstName,
            String formTitle,
            String formUrl) {
        return WhatsAppLinkHelper.buildChatUrl(
                recipientPhone,
                buildRegistrationFormMessage(recipientFirstName, formTitle, formUrl));
    }

    private String buildRegistrationFormMessage(
            String recipientFirstName,
            String formTitle,
            String formUrl) {
        String name = recipientFirstName != null && !recipientFirstName.isBlank()
                ? recipientFirstName.trim()
                : "miembro";
        return "Hola " + name + ", completa tu " + formTitle + ": " + formUrl;
    }

    private BroadcastTemplatePurpose resolvePurpose(BroadcastTemplatePurpose purpose) {
        return purpose != null ? purpose : BroadcastTemplatePurpose.GENERAL;
    }

    private BroadcastChannelSettings getOrCreateSettings(Long organizationId, BroadcastChannel channel) {
        return channelSettingsRepository.findByOrganizationIdAndChannel(organizationId, channel)
                .orElseGet(() -> {
                    Organization org = organizationRepository.findById(organizationId)
                            .orElseThrow(() -> new ResourceNotFoundException("Organización no encontrada"));
                    BroadcastChannelSettings settings = new BroadcastChannelSettings();
                    settings.setOrganization(org);
                    settings.setChannel(channel);
                    settings.setEnabled(false);
                    return channelSettingsRepository.save(settings);
                });
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[\\s()-]", "").trim();
    }

    private void requireConfigRole() {
        var user = SecurityUtils.currentUser();
        if (!user.hasRole("GYM_OWNER") && !user.hasRole("RECEPTIONIST")) {
            throw new BusinessException("No tienes permiso para configurar mensajes de difusión");
        }
    }

    private BroadcastChannelSettingsResponse toSettingsResponse(BroadcastChannelSettings settings) {
        return new BroadcastChannelSettingsResponse(
                settings.getChannel(),
                settings.getSenderPhone(),
                settings.isEnabled(),
                settings.isWhatsappWebSessionConfirmed(),
                settings.getUpdatedAt()
        );
    }

    private BroadcastMessageTemplateResponse toTemplateResponse(BroadcastMessageTemplate template) {
        return new BroadcastMessageTemplateResponse(
                template.getId(),
                template.getChannel(),
                template.getName(),
                template.getBody(),
                template.getPurpose(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
