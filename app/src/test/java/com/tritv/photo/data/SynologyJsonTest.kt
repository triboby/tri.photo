package com.tritv.photo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SynologyJsonTest {

    private val listBody = """
        {"success":true,"data":{"list":[
          {"id":329,"filename":"R5_0001.JPG","filesize":31000000,"time":1697040000,"type":"photo",
           "additional":{"resolution":{"width":8192,"height":5464},"orientation":1,
             "thumbnail":{"cache_key":"329_1697100000","m":"ready","xl":"ready","preview":"broken","sm":"ready","unit_id":329},
             "exif":{"camera":"Canon EOS R5","lens":"RF24-70mm F2.8 L IS USM","iso":"100"},
             "address":{"country":"中国","state":"浙江省","city":"湖州市","district":"德清县"}}},
          {"id":330,"filename":"R5_0002.JPG","time":1697040100,"type":"photo",
           "additional":{"resolution":{"width":8192,"height":5464},"orientation":6,
             "thumbnail":{"cache_key":"330_1","xl":"ready"}}},
          {"id":331,"filename":"clip.mp4","time":1697040200,"type":"video",
           "additional":{"resolution":{"width":1920,"height":1080},
             "thumbnail":{"cache_key":"331_1","xl":"ready"},
             "video_meta":{"duration":15000},
             "video_convert":[{"quality":"low"},{"quality":"high"},{"quality":"medium"}]}},
          {"id":332,"filename":"broken.jpg","time":1,"type":"photo",
           "additional":{"thumbnail":{"cache_key":"332_1","xl":"broken"}}}
        ]}}
    """.trimIndent()

    @Test
    fun readsPhotosWithExifAndPlace() {
        val items = SynologyJson.items(listBody)
        val first = items[0]
        assertEquals(329L, first.id)
        assertEquals(MediaItem.Kind.PHOTO, first.kind)
        assertEquals(1697040000L, first.takenAt)
        assertEquals(8192, first.width)
        assertEquals("329_1697100000", first.thumbnailKey)
        assertEquals("Canon EOS R5", first.camera)
        assertEquals("德清县", first.place)
    }

    @Test
    fun appliesRotationFromExifOrientation() {
        val rotated = SynologyJson.items(listBody)[1]
        assertEquals(5464, rotated.width)
        assertEquals(8192, rotated.height)
        assertTrue(rotated.isPortrait)
    }

    @Test
    fun readsVideosAndOrdersQualities() {
        val video = SynologyJson.items(listBody)[2]
        assertTrue(video.isVideo)
        assertEquals(15000L, video.durationMs)
        assertEquals(listOf("high", "medium", "low"), video.videoQualities)
    }

    @Test
    fun skipsItemsWithoutALargeThumbnail() {
        assertEquals(3, SynologyJson.items(listBody).size)
    }

    @Test
    fun reportsErrors() {
        try {
            SynologyJson.data("""{"success":false,"error":{"code":119}}""")
            fail()
        } catch (e: SynologyException) {
            assertEquals(119, e.code)
            assertTrue(e.sessionExpired)
        }
    }

    @Test
    fun readsApiInfo() {
        val apis = SynologyJson.apiInfo("""{"success":true,"data":{"SYNO.Foto.Thumbnail":{"path":"entry.cgi","minVersion":1,"maxVersion":2}}}""")
        assertEquals(ApiEndpoint("entry.cgi", 2), apis["SYNO.Foto.Thumbnail"])
    }

    @Test
    fun readsAlbumName() {
        assertEquals("莫干山", SynologyJson.albumName("""{"success":true,"data":{"list":[{"id":1,"name":"莫干山"}]}}"""))
        assertNull(SynologyJson.albumName("""{"success":true,"data":{"list":[]}}"""))
    }
}
