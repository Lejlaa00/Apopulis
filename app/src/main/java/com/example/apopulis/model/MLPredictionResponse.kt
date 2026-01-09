package com.example.apopulis.model

data class MLPredictionResponse(
    val success: Boolean,
    val data: MLPredictionData
)

data class MLPredictionData(
    val prediction: String,
    val confidence: Double,
    val is_fake: Boolean,
    val probabilities: MLProbabilities
)

data class MLProbabilities(
    val fake: Double,
    val real: Double
)

