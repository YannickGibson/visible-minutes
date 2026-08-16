package com.visible.internationalminutes.model

/**
 * Representation of a SIM card subscription on the device.
 */
data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val slotIndex: Int,
    val isVisibleOrVerizon: Boolean
) {
    val displayTitle: String
        get() {
            return if (carrierName.isNotBlank() && !displayName.equals(carrierName, ignoreCase = true)) {
                "$displayName ($carrierName)"
            } else {
                displayName.ifBlank { carrierName }.ifBlank { "SIM ${slotIndex + 1}" }
            }
        }
}
