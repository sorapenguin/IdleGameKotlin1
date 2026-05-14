package com.example.idlegameapi.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "game_state")
class GameState(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    var user: User,

    var stage: Int = 1,
    var coins: Long = 0,
    var gems: Int = 0,

    @Column(name = "total_attack")
    var totalAttack: Long = 1,

    @Column(name = "weapon_slots")
    var weaponSlots: Int = 5,

    var energy: Int = 10,

    @UpdateTimestamp
    @Column(name = "last_saved_at")
    var lastSavedAt: LocalDateTime = LocalDateTime.now()
)
