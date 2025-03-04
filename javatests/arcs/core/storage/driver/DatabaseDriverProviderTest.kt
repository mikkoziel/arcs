/*
 * Copyright 2020 Google LLC.
 *
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 *
 * Code distributed by Google as part of this project is also subject to an additional IP rights
 * grant found at
 * http://polymer.github.io/PATENTS.txt
 */

package arcs.core.storage.driver

import arcs.core.common.ArcId
import arcs.core.crdt.CrdtEntity
import arcs.core.crdt.CrdtSet
import arcs.core.crdt.CrdtSingleton
import arcs.core.data.EntityType
import arcs.core.data.FieldType
import arcs.core.data.Schema
import arcs.core.data.SchemaFields
import arcs.core.data.SchemaName
import arcs.core.storage.StorageKey
import arcs.core.storage.StorageKeyProtocol
import arcs.core.storage.database.DatabaseManager
import arcs.core.storage.keys.DatabaseStorageKey
import arcs.core.storage.keys.RamDiskStorageKey
import arcs.core.storage.keys.VolatileStorageKey
import arcs.core.storage.referencemode.ReferenceModeStorageKey
import arcs.jvm.storage.database.testutil.FakeDatabaseManager
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class DatabaseDriverProviderTest {

  private var databaseManager: DatabaseManager? = null

  private val provider = DatabaseDriverProvider.configure(databaseFactory())

  @Before
  fun setUp() {
    DatabaseDriverProvider.resetForTests()
    DatabaseDriverProvider.configure(databaseFactory())
  }

  @After
  fun tearDown() {
    databaseManager = null
  }

  @Test
  fun isConfigured_returnsTrue_afterConfigureIsCalled() = runBlockingTest {
    assertThat(provider.isConfigured).isTrue()
  }

  @Test
  fun isConfigured_returnsFalse_whenNotConfigured() = runBlockingTest {
    provider.resetForTests()

    assertThat(provider.isConfigured).isFalse()
  }

  @Test
  fun willSupport_returnsTrue_whenDatabaseKey_found() = runBlockingTest {
    val key = DatabaseStorageKey.Persistent("foo")
    assertThat(provider.willSupport(key)).isTrue()
  }

  @Test
  fun willSupport_returnsTrue_whenReferenceModeKey_found() = runBlockingTest {
    val key = ReferenceModeStorageKey(
      DatabaseStorageKey.Persistent("foo"),
      DatabaseStorageKey.Persistent("bar")
    )
    assertThat(provider.willSupport(key)).isTrue()
  }

  @Test
  fun willSupport_returnsFalse_whenNotDatabaseKey() = runBlockingTest {
    val ramdisk = RamDiskStorageKey("foo")
    val volatile = VolatileStorageKey(ArcId.newForTest("myarc"), "foo")
    val other = object : StorageKey(StorageKeyProtocol.Dummy) {
      override fun toKeyString(): String = "something"
      override fun newKeyWithComponent(component: String): StorageKey = this
    }

    assertThat(provider.willSupport(ramdisk)).isFalse()
    assertThat(provider.willSupport(volatile)).isFalse()
    assertThat(provider.willSupport(other)).isFalse()
  }

  @Test
  fun willSupport_returnsFalse_whenReferenceModeKeyNoDb() = runBlockingTest {
    val key = ReferenceModeStorageKey(
      RamDiskStorageKey("foo"),
      RamDiskStorageKey("bar")
    )
    assertThat(provider.willSupport(key)).isFalse()
  }

  @Test
  fun willSupport_throwsException_refMode_differentDbNames() = runBlockingTest {
    val key = ReferenceModeStorageKey(
      DatabaseStorageKey.Persistent("foo", "dbA"),
      DatabaseStorageKey.Persistent("bar", "dbB")
    )
    assertFailsWith<IllegalStateException> {
      provider.willSupport(key)
    }.also {
      assertThat(it).hasMessageThat()
        .isEqualTo(
          "Database can support ReferenceModeStorageKey only with a single dbName."
        )
    }
  }

  @Test
  fun getDriver_throwsOnInvalidKey_wrongType() = runBlockingTest {
    val volatile = VolatileStorageKey(ArcId.newForTest("myarc"), "foo")

    assertFailsWith<IllegalArgumentException> {
      provider.getDriver(volatile, CrdtEntity.Data::class, DUMMY_ENTITY_TYPE)
    }
  }

  @Test
  fun getDriver_throwsOnInvalidDataClass() = runBlockingTest {
    val key = DatabaseStorageKey.Persistent("foo")

    assertFailsWith<IllegalArgumentException> {
      provider.getDriver(key, Int::class, DUMMY_ENTITY_TYPE)
    }
  }

  @Test
  fun getDriver() = runBlockingTest {
    val key = DatabaseStorageKey.Persistent("foo")

    val entityDriver = provider.getDriver(key, CrdtEntity.Data::class, DUMMY_ENTITY_TYPE)
    assertThat(entityDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(entityDriver.storageKey).isEqualTo(key)

    val setDriver = provider.getDriver(key, CrdtSet.DataImpl::class, DUMMY_ENTITY_TYPE)
    assertThat(setDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(setDriver.storageKey).isEqualTo(key)

    val singletonDriver = provider.getDriver(key, CrdtSingleton.DataImpl::class, DUMMY_ENTITY_TYPE)
    assertThat(singletonDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(singletonDriver.storageKey).isEqualTo(key)
  }

  @Test
  fun getDriver_referenceModeKey() = runBlockingTest {
    val key = ReferenceModeStorageKey(
      DatabaseStorageKey.Persistent("foo"),
      DatabaseStorageKey.Persistent("bar")
    )

    val entityDriver = provider.getDriver(key, CrdtEntity.Data::class, DUMMY_ENTITY_TYPE)
    assertThat(entityDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(entityDriver.storageKey).isEqualTo(key)

    val setDriver = provider.getDriver(key, CrdtSet.DataImpl::class, DUMMY_ENTITY_TYPE)
    assertThat(setDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(setDriver.storageKey).isEqualTo(key)

    val singletonDriver = provider.getDriver(key, CrdtSingleton.DataImpl::class, DUMMY_ENTITY_TYPE)
    assertThat(singletonDriver).isInstanceOf(DatabaseDriver::class.java)
    assertThat(singletonDriver.storageKey).isEqualTo(key)
  }

  @Test
  fun removeAllEntities() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)

    provider.removeAllEntities()

    verify(mockManager).removeAllEntities()
  }

  @Test
  fun removeEntitiesCreatedBetween() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)

    provider.removeEntitiesCreatedBetween(DUMMY_START, DUMMY_END)

    verify(mockManager).removeEntitiesCreatedBetween(DUMMY_START, DUMMY_END)
  }

  @Test
  fun getEntitiesCount_inMemory() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)
    doReturn(DUMMY_START).whenever(mockManager).getEntitiesCount(false)

    val actual = provider.getEntitiesCount(inMemory = true)

    verify(mockManager).getEntitiesCount(persistent = false)
    assertThat(actual).isEqualTo(DUMMY_START)
  }

  @Test
  fun getEntitiesCount_notInMemory() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)
    doReturn(DUMMY_END).whenever(mockManager).getEntitiesCount(true)

    val actual = provider.getEntitiesCount(inMemory = false)

    verify(mockManager).getEntitiesCount(persistent = true)
    assertThat(actual).isEqualTo(DUMMY_END)
  }

  @Test
  fun getStorageSize_inMemory() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)
    doReturn(DUMMY_START).whenever(mockManager).getStorageSize(false)

    val actual = provider.getStorageSize(inMemory = true)

    verify(mockManager).getStorageSize(false)
    assertThat(actual).isEqualTo(DUMMY_START)
  }

  @Test
  fun getStorageSize_notInMemory() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)
    doReturn(DUMMY_START).whenever(mockManager).getStorageSize(true)

    val actual = provider.getStorageSize(inMemory = false)

    verify(mockManager).getStorageSize(true)
    assertThat(actual).isEqualTo(DUMMY_START)
  }

  @Test
  fun isStorageTooLarge() = runBlockingTest {
    val mockManager = mock<DatabaseManager>()
    provider.configure(mockManager)
    doReturn(true).whenever(mockManager).isStorageTooLarge()

    val actual = provider.isStorageTooLarge()

    verify(mockManager).isStorageTooLarge()
    assertThat(actual).isTrue()
  }

  private fun databaseFactory(): DatabaseManager =
    databaseManager ?: FakeDatabaseManager().also { databaseManager = it }

  companion object {
    private val DUMMY_SCHEMA = Schema(
      setOf(SchemaName("mySchema")),
      SchemaFields(
        mapOf("name" to FieldType.Text),
        mapOf("cities_lived_in" to FieldType.Text)
      ),
      "1234a"
    )
    private const val DUMMY_START = 10000L
    private const val DUMMY_END = 20000L
    private val DUMMY_ENTITY_TYPE = EntityType(DUMMY_SCHEMA)
  }
}
