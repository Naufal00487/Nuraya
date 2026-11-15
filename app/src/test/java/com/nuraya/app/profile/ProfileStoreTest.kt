package com.nuraya.app.profile
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],application=Application::class)
class ProfileStoreTest {
    @Test fun localProfilePersistsWithoutAccountAndRejectsInvalidAvatarOrNickname(){
        val context=ApplicationProvider.getApplicationContext<Application>()
        val store=ProfileStore(context);store.preferences.edit().clear().commit()
        assertEquals(LocalProfile(),store.read())
        store.save("  Naufal  ",2)
        assertEquals(LocalProfile("Naufal",2),ProfileStore(context).read())
        listOf({store.save(" ",0)},{store.save("x".repeat(31),1)},{store.save("Ali",3)}).forEach{action->try{action();fail("invalid profile accepted")}catch(_:IllegalArgumentException){}}
        assertEquals(LocalProfile("Naufal",2),store.read())
    }
}
