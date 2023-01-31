package com.android.example

import com.android.example.geofences.getUniqueId
import org.junit.Assert
import org.junit.Test

class UtilsUnitTest {

    @Test
    fun testUniqueID() {
        val uniqueId = getUniqueId().toString()
        if (uniqueId.isEmpty()) Assert.fail()
        else Assert.assertTrue(true)
    }
}