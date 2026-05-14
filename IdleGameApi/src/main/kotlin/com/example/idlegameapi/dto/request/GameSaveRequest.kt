package com.example.idlegameapi.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class GameSaveRequest(
    @field:Min(1, message = "Stage must be at least 1")
    val stage: Int,

    @field:Min(0, message = "Coins cannot be negative")
    val coins: Long,

    @field:Min(0, message = "Gems cannot be negative")
    val gems: Int,

    @field:Min(1, message = "Total attack must be at least 1")
    val totalAttack: Long,

    @field:Min(5, message = "Weapon slots minimum is 5")
    @field:Max(50, message = "Weapon slots maximum is 50")
    val weaponSlots: Int,

    @field:Min(0, message = "Energy cannot be negative")
    @field:Max(10, message = "Energy maximum is 10")
    val energy: Int
)
