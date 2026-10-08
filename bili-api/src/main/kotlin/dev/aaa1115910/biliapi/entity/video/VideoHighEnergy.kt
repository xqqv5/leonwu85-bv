package dev.aaa1115910.biliapi.entity.video

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

data class VideoHighEnergy(
    val stepSeconds: Int,
    val values: List<Float>,
) {
    companion object {
        fun fromJson(element: JsonElement): VideoHighEnergy? {
            val root = element as? JsonObject ?: return null
            val payloads = listOf(root) + (root["modules"] as? JsonArray).orEmpty().mapNotNull { module ->
                val params = (module as? JsonObject)?.get("params") as? JsonObject
                params?.get("data") as? JsonObject
            }
            return payloads.firstNotNullOfOrNull { payload ->
                val step = (payload["step_sec"] as? JsonPrimitive)?.intOrNull ?: return@firstNotNullOfOrNull null
                if (step <= 0) return@firstNotNullOfOrNull null
                val events = payload["events"] as? JsonObject ?: return@firstNotNullOfOrNull null
                val samples = events["default"] as? JsonArray ?: return@firstNotNullOfOrNull null
                // Preserve each sample's time slot even if the server returns an invalid value.
                val values = samples.map { sample ->
                    (sample as? JsonPrimitive)?.floatOrNull
                        ?.takeIf { it.isFinite() && it >= 0f } ?: 0f
                }
                if (values.size < 2 || values.none { it > 0f }) return@firstNotNullOfOrNull null
                VideoHighEnergy(stepSeconds = step, values = values)
            }
        }
    }
}
