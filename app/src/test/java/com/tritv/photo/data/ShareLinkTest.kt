package com.tritv.photo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareLinkTest {

    @Test
    fun readsTheLinkFromTheNas() {
        val link = ShareLink.parse("http://192.168.50.94:5000/mo/sharing/tZ3xYyQ5C")!!
        assertEquals("http://192.168.50.94:5000", link.baseUrl)
        assertEquals("tZ3xYyQ5C", link.passphrase)
        assertEquals("http://192.168.50.94:5000/mo/sharing/tZ3xYyQ5C", link.sharingPage)
    }

    @Test
    fun assumesHttpAndTrimsSpaces() {
        val link = ShareLink.parse("  192.168.50.94:5000/mo/sharing/abc123 ")!!
        assertEquals("http://192.168.50.94:5000", link.baseUrl)
        assertEquals("abc123", link.passphrase)
    }

    @Test
    fun dropsWhatFollowsTheCode() {
        assertEquals("abc", ShareLink.parse("https://nas.example.com/mo/sharing/abc/?lang=zh#x")!!.passphrase)
        assertEquals("abc", ShareLink.parse("https://nas.example.com/mo/sharing/abc#/item/3")!!.passphrase)
    }

    @Test
    fun keepsThePhotoPrefix() {
        val link = ShareLink.parse("https://nas.example.com:5001/photo/mo/sharing/Xy_9-z")!!
        assertEquals("https://nas.example.com:5001", link.baseUrl)
        assertEquals("https://nas.example.com:5001/photo/mo/sharing/Xy_9-z", link.sharingPage)
    }

    @Test
    fun rejectsOtherText() {
        assertNull(ShareLink.parse(""))
        assertNull(ShareLink.parse("http://192.168.50.94:5000/"))
        assertNull(ShareLink.parse("http://192.168.50.94:5000/mo/sharing/"))
        assertNull(ShareLink.parse("hello"))
    }
}
