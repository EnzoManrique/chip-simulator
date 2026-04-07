package com.manrique.chipsimulator.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount")
    @Builder.Default
    private Integer amount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToMany
    @JoinTable(
            name = "pot_eligible_players",
            joinColumns = @JoinColumn(name = "pot_id"),
            inverseJoinColumns = @JoinColumn(name = "room_player_id")
    )
    @Builder.Default
    private List<RoomPlayer> eligiblePlayers = new ArrayList<>();
}
