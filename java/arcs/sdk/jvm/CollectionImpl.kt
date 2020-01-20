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

package arcs.sdk

/** [ReadWriteCollection] implementation for the JVM. */
@Suppress("UNUSED_PARAMETER")
// TODO: Connect to storage.
class CollectionImpl<T : Entity>(
    val particle: Particle,
    override val name: String,
    entitySpec: EntitySpec<T>
) : ReadWriteCollection<T> {
    private val entities = mutableListOf<T>()

    override val size: Int
        get() = entities.size

    override fun isEmpty(): Boolean = entities.isEmpty()

    override fun iterator(): Iterator<T> = entities.iterator()

    override fun store(entity: T) {
        entities.add(entity)
        particle.onHandleUpdate(this)
    }

    override fun clear() {
        entities.clear()
        particle.onHandleUpdate(this)
    }

    override fun remove(entity: T) {
        entities.remove(entity)
        particle.onHandleUpdate(this)
    }
}