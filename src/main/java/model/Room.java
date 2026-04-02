package model;

import jakarta.persistence.*;
import lombok.*;
import model.enums.RoomStatus;
import model.enums.RoomPhase;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase")
    @Builder.Default
    private RoomPhase phase = RoomPhase.PRE_FLOP;

    @Column(name = "initial_chips")
    @Builder.Default
    private Integer initialChips = 1000;

    @Column(name = "pot")
    @Builder.Default
    private Integer pot = 0;

    @Column(name = "dealer_seat")
    @Builder.Default
    private Integer dealerSeat = 0;

    @Column(name = "turn_seat")
    @Builder.Default
    private Integer turnSeat = 0;

    @Column(name = "highest_bet")
    @Builder.Default
    private Integer highestBet = 0;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RoomPlayer> players = new ArrayList<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ActionLog> actionLogs = new ArrayList<>();
}
