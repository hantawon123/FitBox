package com.ssafy.fitbox.util

data class AddressParts(
    val zoneCode: String = "",
    val roadAddress: String = "",
    val detailAddress: String = ""
) {
    val roadAddressWithZone: String
        get() = if (zoneCode.isBlank()) {
            roadAddress
        } else {
            "[$zoneCode] $roadAddress"
        }.trim()

    val fullAddress: String
        get() = compose(zoneCode, roadAddress, detailAddress)

    companion object {
        private val zoneCodeRegex = Regex("""^\[([0-9]{5})]\s*(.*)$""")
        private val roadAddressRegex = Regex(
            pattern = """^(.+?(?:대로|로|길)\s+\d+(?:-\d+)?)(?:\s+(.+))?$"""
        )

        fun compose(
            zoneCode: String,
            roadAddress: String,
            detailAddress: String
        ): String {
            val normalizedZoneCode = zoneCode.trim().trim('[', ']')
            val normalizedRoadAddress = roadAddress
                .replace("\\s+".toRegex(), " ")
                .trim()
            val normalizedDetailAddress = detailAddress
                .replace("\\s+".toRegex(), " ")
                .trim()

            val roadWithZone = if (normalizedZoneCode.isBlank()) {
                normalizedRoadAddress
            } else {
                "[$normalizedZoneCode] $normalizedRoadAddress"
            }.trim()

            return if (normalizedDetailAddress.isBlank()) {
                roadWithZone
            } else {
                "$roadWithZone\n상세주소: $normalizedDetailAddress"
            }
        }

        fun parse(fullAddress: String): AddressParts {
            val normalized = fullAddress
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim()

            if (normalized.isBlank()) {
                return AddressParts()
            }

            val lines = normalized
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val firstLine = lines.firstOrNull().orEmpty()
            val explicitDetail = lines.drop(1)
                .joinToString(" ")
                .removePrefix("상세주소:")
                .trim()

            val zoneMatch = zoneCodeRegex.find(firstLine)
            val zoneCode = zoneMatch?.groupValues?.getOrNull(1).orEmpty()
            val addressWithoutZone = zoneMatch?.groupValues?.getOrNull(2)?.trim()
                ?: firstLine

            if (explicitDetail.isNotBlank()) {
                return AddressParts(
                    zoneCode = zoneCode,
                    roadAddress = addressWithoutZone,
                    detailAddress = explicitDetail
                )
            }

            val roadMatch = roadAddressRegex.find(addressWithoutZone)
            if (roadMatch != null) {
                return AddressParts(
                    zoneCode = zoneCode,
                    roadAddress = roadMatch.groupValues.getOrNull(1).orEmpty().trim(),
                    detailAddress = roadMatch.groupValues.getOrNull(2).orEmpty().trim()
                )
            }

            return AddressParts(
                zoneCode = zoneCode,
                roadAddress = addressWithoutZone,
                detailAddress = ""
            )
        }
    }
}
