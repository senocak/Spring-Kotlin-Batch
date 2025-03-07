package com.github.senocak.model

import jakarta.persistence.*

@Entity
@Table(name = "traffic_density")
data class TrafficDensity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val dateTime: String,
    val latitude: String,
    val longitude: String,
    val geohash: String,
    val minimumSpeed: Int,
    val maximumSpeed: Int,
    val averageSpeed: Int,
    val numberOfVehicles: Int,
)
