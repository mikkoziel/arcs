load("//third_party/java/arcs/build_defs:build_defs.bzl", "arcs_kt_android_library")

licenses(["notice"])

package(default_visibility = ["//java/arcs:allowed-packages"])

arcs_kt_android_library(
    name = "database",
    srcs = glob(["*.kt"]),
    manifest = "//java/arcs/android/common:AndroidManifest.xml",
    deps = [
        "//java/arcs/android/common",  # buildcleaner: keep
        "//java/arcs/android/crdt",
        "//java/arcs/android/crdt:version_map_android_proto",
        "//java/arcs/core/common",
        "//java/arcs/core/crdt",
        "//java/arcs/core/data",
        "//java/arcs/core/data:rawentity",
        "//java/arcs/core/data:schema_fields",
        "//java/arcs/core/data/util:data-util",
        "//java/arcs/core/storage:reference",
        "//java/arcs/core/storage:storage_key",
        "//java/arcs/core/storage/database",
        "//java/arcs/core/storage/driver:driver_providers",
        "//java/arcs/core/util",
        "//java/arcs/core/util:utils-platform-dependencies",
        "//java/arcs/core/util/performance",
        "//java/arcs/flags",
        "//java/arcs/flags:build_flags",
        "//java/arcs/jvm/util",
        "//third_party/java/androidx/annotation",
        "//third_party/java/androidx/lifecycle",
        "//third_party/java/androidx/work",
        "//third_party/java/arcs/deps:protobuf_javalite",
        "//third_party/kotlin/kotlinx_atomicfu",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)
