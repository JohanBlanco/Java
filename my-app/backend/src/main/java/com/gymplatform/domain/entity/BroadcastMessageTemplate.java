package com.gymplatform.domain.entity;

import com.gymplatform.domain.enums.BroadcastChannel;
import com.gymplatform.domain.enums.BroadcastTemplatePurpose;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "broadcast_message_templates")
public class BroadcastMessageTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastChannel channel;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 4096)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastTemplatePurpose purpose = BroadcastTemplatePurpose.GENERAL;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public BroadcastChannel getChannel() { return channel; }
    public void setChannel(BroadcastChannel channel) { this.channel = channel; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public BroadcastTemplatePurpose getPurpose() { return purpose; }
    public void setPurpose(BroadcastTemplatePurpose purpose) { this.purpose = purpose; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
