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

package arcs.android.crdt

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import arcs.android.util.writeProto
import arcs.core.crdt.CrdtEntity
import arcs.core.crdt.CrdtSet
import arcs.core.crdt.CrdtSingleton
import arcs.core.crdt.VersionMap
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParcelableCrdtEntityTest {
  private val versionMap: VersionMap = VersionMap("alice" to 1, "bob" to 3)

  private val referenceA: CrdtEntity.Reference = CrdtEntity.ReferenceImpl("AAA")
  private val referenceB: CrdtEntity.Reference = CrdtEntity.ReferenceImpl("BBB")
  private val referenceC: CrdtEntity.Reference = CrdtEntity.ReferenceImpl("CCC")

  @Test
  fun referenceImpl_parcelableRoundTrip_works() {
    val reference = CrdtEntity.ReferenceImpl("ref")
    val unmarshalled = roundTripThroughParcel(
      reference.toProto(),
      Parcel::writeProto,
      Parcel::readCrdtEntityReference
    )

    assertThat(unmarshalled).isEqualTo(reference)
  }

  @Test
  fun data_parcelableRoundTrip_works() {
    val data = CrdtEntity.Data(
      VersionMap("alice" to 1, "bob" to 2),
      singletons = mapOf(
        "a" to CrdtSingleton(VersionMap("alice" to 1), referenceA),
        "b" to CrdtSingleton(VersionMap("bob" to 1), referenceB)
      ),
      collections = mapOf(
        "c" to CrdtSet.createWithData(
          CrdtSet.DataImpl(
            VersionMap("bob" to 3),
            mutableMapOf(
              "CCC" to CrdtSet.DataValue(
                VersionMap("bob" to 2),
                referenceC
              )
            )
          )
        )
      )
    )

    invariant_CrdtData_preservedDuring_parcelRoundTrip(data)
  }

  @Test
  fun operationSetSingleton_parcelableRoundTrip_works() {
    val op = CrdtEntity.Operation.SetSingleton("alice", versionMap, "field", referenceA)
    invariant_CrdtOperation_preservedDuring_parcelRoundTrip(op)
  }

  @Test
  fun operationClearSingleton_parcelableRoundTrip_works() {
    val op = CrdtEntity.Operation.ClearSingleton("alice", versionMap, "field")
    invariant_CrdtOperation_preservedDuring_parcelRoundTrip(op)
  }

  @Test
  fun operationAddToSet_parcelableRoundTrip_works() {
    val op = CrdtEntity.Operation.AddToSet("alice", versionMap, "field", referenceA)
    invariant_CrdtOperation_preservedDuring_parcelRoundTrip(op)
  }

  @Test
  fun operationRemoveFromSet_parcelableRoundTrip_works() {
    val op = CrdtEntity.Operation.RemoveFromSet("alice", versionMap, "field", referenceA.id)
    invariant_CrdtOperation_preservedDuring_parcelRoundTrip(op)
  }

  @Test
  fun multipleOperations_crdtEntity_parcelableRoundTrip_works() {
    val ops = listOf(
      CrdtEntity.Operation.SetSingleton("alice", versionMap, "field", referenceA),
      CrdtEntity.Operation.ClearSingleton("alice", versionMap, "field"),
      CrdtEntity.Operation.AddToSet("alice", versionMap, "field", referenceA),
      CrdtEntity.Operation.RemoveFromSet("alice", versionMap, "field", referenceA.id)
    )
    invariant_CrdtOperations_preservedDuring_parcelRoundTrip(ops)
  }
}
