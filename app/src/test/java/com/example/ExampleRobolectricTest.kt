package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AppConstants
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and database id from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val dbId = context.getString(R.string.firestore_database_id)
        assertEquals("MCQ Learning App", appName)
        assertEquals("(default)", dbId)
        assertEquals("aaWqMj9EgnWbZbX3skVUMOgpgRq2", AppConstants.SUPER_ADMIN_UID)
    }

    @Test
    fun `main activity launches cleanly without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apps = FirebaseApp.getApps(context)
        assertNotNull(apps)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }
}
