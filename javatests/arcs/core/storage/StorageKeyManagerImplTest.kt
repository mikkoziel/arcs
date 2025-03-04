package arcs.core.storage

import arcs.core.storage.testutil.DummyStorageKey
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class StorageKeyManagerImplTest() {

  @Test
  fun init_registersParser() {
    val storageKeyManager = StorageKeyManagerImpl(DummyStorageKey)

    assertThat(storageKeyManager.parse(dummyStorageKeyStr)).isEqualTo(dummyStorageKey)
  }

  @Test
  fun addParser_registersParser() {
    val storageKeyManager = StorageKeyManagerImpl()

    storageKeyManager.addParser(DummyStorageKey)

    assertThat(storageKeyManager.parse(dummyStorageKeyStr)).isEqualTo(dummyStorageKey)
  }

  @Test
  fun parse_missingProtocol_throws() {
    val storageKeyManager = StorageKeyManagerImpl()

    val e = assertFailsWith<IllegalArgumentException> { storageKeyManager.parse("no-protocol") }
    assertThat(e).hasMessageThat().isEqualTo("Invalid key pattern: no-protocol")
  }

  @Test
  fun parse_unknownLongProtocol_throws() {
    val storageKeyManager = StorageKeyManagerImpl()

    val e = assertFailsWith<IllegalArgumentException> { storageKeyManager.parse("unknown://abc") }
    assertThat(e).hasMessageThat().isEqualTo("Unknown storage key protocol: unknown")
  }

  @Test
  fun parse_flagOn_unknownShortProtocol_throws() {
    val storageKeyManager = StorageKeyManagerImpl()

    val e = assertFailsWith<IllegalArgumentException> { storageKeyManager.parse("x|abc") }
    assertThat(e).hasMessageThat().isEqualTo("Unknown storage key protocol: x")
  }

  @Test
  fun parse_unregisteredProtocol_throws() {
    val storageKeyManager = StorageKeyManagerImpl()

    val e = assertFailsWith<IllegalArgumentException> { storageKeyManager.parse("dummy://abc") }

    assertThat(e).hasMessageThat().isEqualTo(
      "No registered parsers for protocol \"${StorageKeyProtocol.Dummy}\""
    )
  }

  @Test
  fun reset_resetsToDefaults() {
    val storageKeyManager = StorageKeyManagerImpl(DummyStorageKey)

    storageKeyManager.reset()

    val e = assertFailsWith<IllegalArgumentException> {
      storageKeyManager.parse(dummyStorageKeyStr)
    }

    assertThat(e).hasMessageThat().isEqualTo(
      "No registered parsers for protocol \"${StorageKeyProtocol.Dummy}\""
    )
  }

  /**
   * Tests that [StorageKeyManagerImpl] is thread safe. Adds and resets parsers in one thread, and
   * parses in another.
   */
  @Test
  fun testRegistrationRacing() = runBlocking<Unit> {
    val storageKeyManager = StorageKeyManagerImpl(DummyStorageKey)
    val threadOne = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val threadTwo = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    launch(threadOne) {
      (1..1000).forEach { _ ->
        storageKeyManager.reset(DummyStorageKey)
        storageKeyManager.addParser(DummyStorageKey)
      }
    }
    launch(threadTwo) {
      (1..1000).forEach { _ ->
        assertThat(storageKeyManager.parse(dummyStorageKeyStr)).isEqualTo(dummyStorageKey)
      }
    }
  }

  private companion object {
    // Implemented using getter methods since the representation changes according to build flags.
    private val dummyStorageKey get() = DummyStorageKey("abc")
    private val dummyStorageKeyStr get() = dummyStorageKey.toString()
  }
}
