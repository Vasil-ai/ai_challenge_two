package com.events.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "host_memberships")
@IdClass(HostMembership.HostMembershipId.class)
@Getter
@Setter
@NoArgsConstructor
public class HostMembership {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "host_id")
    private Long hostId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id", insertable = false, updatable = false)
    private Host host;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipRole role;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class HostMembershipId implements Serializable {
        private Long userId;
        private Long hostId;
    }
}
