package dev.aaa1115910.bv.viewmodel

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.DashAudio
import dev.aaa1115910.biliapi.entity.DashVideo
import dev.aaa1115910.biliapi.entity.PlayData
import dev.aaa1115910.biliapi.entity.PlayDataUnavailableException
import dev.aaa1115910.biliapi.http.entity.VVoucherException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VodPlayDataResolverTest {
    @Test
    fun `single track response from either api is accepted without fallback`() = runBlocking {
        val videoOnly = playableData().copy(dashAudios = emptyList())
        val audioOnly = playableData().copy(dashVideos = emptyList())
        for (api in ApiType.entries) {
            for (expected in listOf(videoOnly, audioOnly)) {
                val attempts = mutableListOf<ApiType>()
                val actual = resolvePlayableVodPlayData(preferredApi = api, fetch = {
                    attempts += it
                    expected
                })

                assertSame(expected, actual)
                assertEquals(listOf(api), attempts)
            }
        }
    }

    @Test
    fun `fallback accepts either a video only or audio only response`() = runBlocking {
        for (expected in listOf(
            playableData().copy(dashAudios = emptyList()),
            playableData().copy(dashVideos = emptyList()),
        )) {
            val attempts = mutableListOf<ApiType>()
            val actual = resolvePlayableVodPlayData(preferredApi = ApiType.App, fetch = { api ->
                attempts += api
                if (api == ApiType.App) emptyData() else expected
            })

            assertSame(expected, actual)
            assertEquals(listOf(ApiType.App, ApiType.Web), attempts)
        }
    }

    @Test
    fun `app failure falls back to web`() = runBlocking {
        val expected = playableData()
        val attempts = mutableListOf<ApiType>()

        val actual = resolvePlayableVodPlayData(preferredApi = ApiType.App, fetch = { api ->
            attempts += api
            when (api) {
                ApiType.App -> throw PlayDataUnavailableException("grpc unavailable")
                ApiType.Web -> expected
            }
        })

        assertSame(expected, actual)
        assertEquals(listOf(ApiType.App, ApiType.Web), attempts)
    }

    @Test
    fun `app empty response falls back to web`() = runBlocking {
        val expected = playableData()
        val attempts = mutableListOf<ApiType>()

        val actual = resolvePlayableVodPlayData(preferredApi = ApiType.App, fetch = { api ->
            attempts += api
            when (api) {
                ApiType.App -> emptyData()
                ApiType.Web -> expected
            }
        })

        assertSame(expected, actual)
        assertEquals(listOf(ApiType.App, ApiType.Web), attempts)
    }

    @Test
    fun `both empty responses produce expected unavailable error`() {
        val error = assertFailsWith<PlayDataUnavailableException> {
            runBlocking {
                resolvePlayableVodPlayData(preferredApi = ApiType.Web, fetch = { emptyData() })
            }
        }

        assertTrue(error.message?.contains("WEB、APP 接口均未返回") == true)
    }

    @Test
    fun `risk voucher does not get hidden by fallback`() {
        val attempts = mutableListOf<ApiType>()

        assertFailsWith<VVoucherException> {
            runBlocking {
                resolvePlayableVodPlayData(preferredApi = ApiType.Web, fetch = { api ->
                    attempts += api
                    throw VVoucherException("voucher")
                })
            }
        }

        assertEquals(listOf(ApiType.Web), attempts)
    }

    @Test
    fun `fallback risk voucher is propagated`() {
        val attempts = mutableListOf<ApiType>()

        val error = assertFailsWith<VVoucherException> {
            runBlocking {
                resolvePlayableVodPlayData(preferredApi = ApiType.App, fetch = { api ->
                    attempts += api
                    when (api) {
                        ApiType.App -> throw PlayDataUnavailableException("grpc unavailable")
                        ApiType.Web -> throw VVoucherException("voucher_from_web")
                    }
                })
            }
        }

        assertEquals("voucher_from_web", error.vVoucher)
        assertEquals(listOf(ApiType.App, ApiType.Web), attempts)
    }

    private fun emptyData() = PlayData(
        dashVideos = emptyList(),
        dashAudios = emptyList()
    )

    private fun playableData() = PlayData(
        dashVideos = listOf(
            DashVideo(
                quality = 80,
                baseUrl = "https://example.com/video.m4s",
                bandwidth = 1,
                codecId = 7,
                width = 1920,
                height = 1080,
                frameRate = "30",
                backUrl = emptyList(),
                codecs = "avc1"
            )
        ),
        dashAudios = listOf(
            DashAudio(
                baseUrl = "https://example.com/audio.m4s",
                bandwidth = 1,
                codecId = 30216,
                backUrl = emptyList()
            )
        )
    )
}
