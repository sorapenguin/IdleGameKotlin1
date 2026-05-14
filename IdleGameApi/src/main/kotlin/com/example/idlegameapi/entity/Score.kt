package com.example.idlegameapi.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "score")
class Score(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    var user: User,

    @Column(name = "total_kills")
    var totalKills: Long = 0,

    @Column(name = "max_stage_reached")
    var maxStageReached: Int = 1,

    @Column(name = "total_coins_earned")
    var totalCoinsEarned: Long = 0,

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
