package com.example.idlegameapi.controller

import com.example.idlegameapi.dto.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/time")
class TimeController {

    @GetMapping
    fun serverTime(): ApiResponse<Long> =
        ApiResponse(success = true, message = "ok", data = System.currentTimeMillis())
}
