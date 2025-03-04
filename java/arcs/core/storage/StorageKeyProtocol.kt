package arcs.core.storage

/** All [StorageKey] protocols understood by Arcs. */
enum class StorageKeyProtocol(
  /** Full name of the protocol, e.g. "ramdisk", "reference-mode". */
  private val longId: String,
  /** Short, single character identifier for the protocol. */
  private val shortId: String
) {
  // Sorted alphabetically by shortProtocol.
  Create("create", "c"),
  Database("db", "d"),
  ReferenceMode("reference-mode", "e"),
  Foreign("foreign", "f"),
  Inline("inline", "i"),
  Join("join", "j"),
  InMemoryDatabase("memdb", "m"),
  RamDisk("ramdisk", "r"),
  Dummy("dummy", "u"),
  Volatile("volatile", "v");

  val id = shortId
  val protocol = "$shortId|"

  override fun toString() = protocol

  companion object {
    private val SHORT_PROTOCOLS = values().associateBy { it.shortId }
    private val LONG_PROTOCOLS = values().associateBy { it.longId }

    fun parseProtocol(protocol: String): StorageKeyProtocol {
      // Try short protocol first.
      SHORT_PROTOCOLS[protocol]?.let { return it }

      // Fall back to long protocol.
      return LONG_PROTOCOLS[protocol] ?: throw IllegalArgumentException(
        "Unknown storage key protocol: $protocol"
      )
    }
  }
}
