package info.meuse24.m24bikestats.testsupport

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import info.meuse24.m24bikestats.auth.CachedOidcDiscoveryInfo
import info.meuse24.m24bikestats.auth.CachedOidcUserInfo
import info.meuse24.m24bikestats.auth.OidcCacheRepository
import info.meuse24.m24bikestats.data.local.dao.ActivityDao
import info.meuse24.m24bikestats.data.local.dao.ActivityDetailDao
import info.meuse24.m24bikestats.data.local.dao.BikeDao
import info.meuse24.m24bikestats.data.local.database.BoschDatabase
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeOidcCacheRepository : OidcCacheRepository {
    var storedUserInfo: CachedOidcUserInfo? = null
    var storedDiscoveryInfo: CachedOidcDiscoveryInfo? = null
    var clearCalls: Int = 0

    override fun getCachedUserInfo(): CachedOidcUserInfo? = storedUserInfo

    override fun saveCachedUserInfo(info: CachedOidcUserInfo) {
        storedUserInfo = info
    }

    override fun getCachedDiscoveryInfo(): CachedOidcDiscoveryInfo? = storedDiscoveryInfo

    override fun saveCachedDiscoveryInfo(info: CachedOidcDiscoveryInfo) {
        storedDiscoveryInfo = info
    }

    override fun clearOidcCache() {
        clearCalls += 1
        storedUserInfo = null
        storedDiscoveryInfo = null
    }
}

class TestBoschDatabase(
    private val activityCountProvider: () -> Int = { 0 },
) : BoschDatabase() {
    var clearAllTablesCalls: Int = 0

    private val activityDaoProxy: ActivityDao = createDaoProxy { method, _ ->
        when (method.name) {
            "count" -> activityCountProvider()
            else -> defaultDaoReturnValue(method, null)
        }
    }
    private val activityDetailDaoProxy: ActivityDetailDao =
        createDaoProxy { method, args -> defaultDaoReturnValue(method, args) }
    private val bikeDaoProxy: BikeDao =
        createDaoProxy { method, args -> defaultDaoReturnValue(method, args) }

    override fun activityDao(): ActivityDao = activityDaoProxy

    override fun activityDetailDao(): ActivityDetailDao = activityDetailDaoProxy

    override fun bikeDao(): BikeDao = bikeDaoProxy

    override fun createOpenDelegate(): RoomOpenDelegate =
        error("TestBoschDatabase does not open a real database")

    override fun createInvalidationTracker(): InvalidationTracker =
        InvalidationTracker(this, mutableMapOf(), mutableMapOf())

    override fun clearAllTables() {
        clearAllTablesCalls += 1
    }

    override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> = emptyMap()

    override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> = emptySet()

    override fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>,
    ): List<Migration> = emptyList()
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T> createDaoProxy(
    crossinline handler: (Method, Array<Any?>?) -> Any?,
): T = Proxy.newProxyInstance(
    T::class.java.classLoader,
    arrayOf(T::class.java),
) { _, method, args ->
    handler(method, args)
} as T

private fun defaultDaoReturnValue(method: Method, args: Array<Any?>?): Any? = when {
    method.name == "equals" -> false
    method.name == "hashCode" -> 0
    method.name == "toString" -> method.declaringClass.simpleName
    method.returnType == java.lang.Boolean.TYPE -> false
    method.returnType == java.lang.Integer.TYPE -> 0
    method.returnType == java.lang.Long.TYPE -> 0L
    method.returnType == java.lang.Float.TYPE -> 0f
    method.returnType == java.lang.Double.TYPE -> 0.0
    method.returnType == java.lang.Void.TYPE -> null
    Flow::class.java.isAssignableFrom(method.returnType) -> emptyFlow<Any?>()
    List::class.java.isAssignableFrom(method.returnType) -> emptyList<Any?>()
    Set::class.java.isAssignableFrom(method.returnType) -> emptySet<Any?>()
    Map::class.java.isAssignableFrom(method.returnType) -> emptyMap<Any?, Any?>()
    args != null && args.lastOrNull()?.javaClass?.name?.contains("Continuation") == true -> null
    else -> null
}
