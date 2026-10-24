package com.tritv.photo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCodecTest {

    @Test
    fun roundTrips() {
        val album = Album(
            name = "莫干山",
            items = listOf(
                MediaItem(1, MediaItem.Kind.PHOTO, "a.jpg", 100, 4000, 3000, "1_1", camera = "Canon EOS R5", place = "德清县"),
                MediaItem(2, MediaItem.Kind.VIDEO, "b.mp4", 200, 1920, 1080, "2_1", videoQualities = listOf("high", "medium"), durationMs = 9000),
            ),
            fetchedAt = 12345,
        )
        val back = AlbumCodec.decode(AlbumCodec.encode(album))
        assertEquals(album.name, back.name)
        assertEquals(album.items, back.items)
        assertEquals(12345L, back.fetchedAt)
        assertTrue(back.fromCache)
    }
}
