package com.wbtracker.app.domain.model

data class ReviewStats(
    val currentRating: Double,
    val currentReviewsCount: Int,
    val minRating: Double,
    val maxRating: Double,
    val avgRating: Double,
    val reviewHistory: List<ReviewPoint>
)

data class ReviewPoint(
    val timestamp: Long,
    val rating: Double,
    val reviewsCount: Int
)
