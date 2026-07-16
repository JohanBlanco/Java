package com.gymplatform.domain.entity;

import com.gymplatform.domain.enums.BroadcastChannel;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "broadcast_channel_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "channel"})
)
public class BroadcastChannelSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastChannel channel;

    /** Número remitente en formato internacional, p. ej. +5215512345678 */
    @Column(length = 32)
    private String senderPhone;

    @Column(nullable = false)
    private boolean enabled = false;

    /** El admin confirmó tener sesión activa en WhatsApp Web en su equipo. */
    @Column(nullable = false)
    private boolean whatsappWebSessionConfirmed = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public BroadcastChannel getChannel() { return channel; }
    public void setChannel(BroadcastChannel channel) { this.channel = channel; }
    public String getSenderPhone() { return senderPhone; }
    public void setSenderPhone(String senderPhone) { this.senderPhone = senderPhone; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isWhatsappWebSessionConfirmed() { return whatsappWebSessionConfirmed; }
    public void setWhatsappWebSessionConfirmed(boolean whatsappWebSessionConfirmed) {
        this.whatsappWebSessionConfirmed = whatsappWebSessionConfirmed;
    }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
