package dev.aaa1115910.biliapi.entity

import dev.aaa1115910.biliapi.http.entity.video.Durl
import dev.aaa1115910.biliapi.http.entity.video.Dash
import dev.aaa1115910.biliapi.http.entity.video.DashData
import dev.aaa1115910.biliapi.http.entity.video.DashDolby
import dev.aaa1115910.biliapi.http.entity.video.DashFlac
import dev.aaa1115910.biliapi.http.entity.video.PlayUrlData
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayDataTest {
    @Test
    fun `BV1Vez3YoEiA video remains playable with null or empty dash audio`() {
        // Public playurl response reduced to one AVC track; CDN URLs are replaced.
        for (audioJson in listOf("null", "[]")) {
            val response = Json.decodeFromString<PlayUrlData>(
                """
                {
                  "quality": 64,
                  "timelength": 292599,
                  "dash": {
                    "duration": 293,
                    "video": [{
                      "id": 32,
                      "base_url": "https://example.com/video.m4s",
                      "backup_url": ["https://backup.example.com/video.m4s"],
                      "bandwidth": 269849,
                      "codecid": 7,
                      "codecs": "avc1.64001E",
                      "mime_type": "video/mp4",
                      "width": 480,
                      "height": 480,
                      "frame_rate": "30.000",
                      "segment_base": {"initialization": "0-943", "index_range": "944-1683"}
                    }],
                    "audio": $audioJson,
                    "dolby": {"type": 0, "audio": null},
                    "flac": null
                  }
                }
                """.trimIndent()
            )
            val playData = PlayData.fromPlayUrlData(response)

            assertTrue(playData.hasPlayableVodStreams())
            assertFalse(playData.hasMuxedVideo())
            assertEquals(0, playData.playableAudioCount())
            assertEquals(292_599L, playData.timeLength)
            assertEquals("944-1683", playData.dashVideos.single().indexRange)
        }
    }

    @Test
    fun `audio only dash response remains playable`() {
        // Match the HTTP client's coercion of null video lists to the model's empty default.
        val json = Json { coerceInputValues = true }
        for (videoField in listOf("\"video\": null,", "\"video\": [],", "")) {
            val response = json.decodeFromString<PlayUrlData>(
                """
                {"dash": {
                  "duration": 60,
                  $videoField
                  "audio": [{"id": 30216, "base_url": "${audioData.baseUrl}", "codecs": "mp4a.40.2"}]
                }}
                """.trimIndent()
            )
            val playData = PlayData.fromPlayUrlData(response)

            assertTrue(playData.hasPlayableVodStreams())
            assertTrue(playData.dashVideos.isEmpty())
            assertEquals(60_000L, playData.timeLength)
            assertEquals(audioData.baseUrl, playData.dashAudios.single().baseUrl)
        }
    }

    @Test
    fun `audio only dolby and flac responses remain playable`() {
        for (dash in listOf(
            Dash(dolby = DashDolby(audio = listOf(audioData.copy(id = 30250)))),
            Dash(flac = DashFlac(display = true, audio = audioData.copy(id = 30251))),
        )) {
            val playData = PlayData.fromPlayUrlData(PlayUrlData(dash = dash))

            assertTrue(playData.hasPlayableVodStreams())
            assertTrue(playData.dashVideos.isEmpty())
            assertTrue(playData.dashAudios.isEmpty())
            assertEquals(1, playData.playableAudioCount())
        }
    }

    @Test
    fun `stream entries without usable urls remain unplayable`() {
        val playData = PlayData.fromPlayUrlData(
            PlayUrlData(dash = Dash(video = listOf(DashData()), audio = listOf(audioData.copy(baseUrl = " "))))
        )

        assertFalse(playData.hasPlayableVodStreams())
    }

    @Test
    fun `legacy durl is exposed as a playable muxed stream`() {
        val playData = PlayData.fromPlayUrlData(
            PlayUrlData(
                quality = 16,
                videoCodecId = 7,
                timeLength = 12_345,
                durl = listOf(
                    Durl(
                        order = 1,
                        length = 12_345,
                        size = 1_024,
                        ahead = "",
                        vhead = "",
                        url = "https://example.com/video.mp4",
                        backupUrl = listOf("https://backup.example.com/video.mp4")
                    )
                )
            )
        )

        assertTrue(playData.hasPlayableVodStreams())
        assertTrue(playData.hasMuxedVideo())
        assertEquals(0, playData.playableAudioCount())
        assertEquals(1, playData.dashVideos.size)
        assertEquals(16, playData.dashVideos.single().quality)
        assertEquals("avc1", playData.dashVideos.single().codecs)
        assertEquals("https://example.com/video.mp4", playData.dashVideos.single().baseUrl)
        assertEquals(
            listOf("https://backup.example.com/video.mp4"),
            playData.dashVideos.single().backUrl
        )
    }

    @Test
    fun `empty play url response remains unplayable`() {
        val playData = PlayData.fromPlayUrlData(PlayUrlData())

        assertFalse(playData.hasPlayableVodStreams())
        assertFalse(playData.hasMuxedVideo())
        assertTrue(playData.dashVideos.isEmpty())
        assertTrue(playData.dashAudios.isEmpty())
    }

    private val audioData = DashData(
        id = 30216,
        baseUrl = "https://example.com/audio.m4s",
        codecs = "mp4a.40.2",
        mimeType = "audio/mp4",
    )
}
