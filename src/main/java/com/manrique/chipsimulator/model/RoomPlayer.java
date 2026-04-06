package com.manrique.chipsimulator.model;

import jakarta.persistence.*;
import lombok.*;
import com.manrique.chipsimulator.model.enums.PlayerStatus;

@Entity
@Table(name = "room_players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "chips_balance")
    @Builder.Default
    private Integer chipsBalance = 0;

    @Column(name = "current_bet")
    @Builder.Default
    private Integer currentBet = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private PlayerStatus status = PlayerStatus.ACTIVE;
}
