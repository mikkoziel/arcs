load(
    "//third_party/java/arcs/build_defs:build_defs.bzl",
    "arcs_kt_android_test_suite",
    "arcs_kt_jvm_library",
)

licenses(["notice"])

package(default_visibility = ["//java/arcs:allowed-packages"])

TEST_SRCS = glob(["*Test.kt"])

arcs_kt_android_test_suite(
    name = "storage",
    srcs = TEST_SRCS,
    manifest = "//java/arcs/android/common:AndroidManifest.xml",
    package = "arcs.core.storage",
    deps = [
        ":fakes",
        "//java/arcs/core/analytics",
        "//java/arcs/core/common",
        "//java/arcs/core/crdt",
        "//java/arcs/core/crdt/testing",
        "//java/arcs/core/data",
        "//java/arcs/core/data:schema_fields",
        "//java/arcs/core/data/testutil",
        "//java/arcs/core/data/util:data-util",
        "//java/arcs/core/storage",
        "//java/arcs/core/storage/api",
        "//java/arcs/core/storage/database",
        "//java/arcs/core/storage/driver:driver_providers",
        "//java/arcs/core/storage/driver:ramdisk",
        "//java/arcs/core/storage/driver/testutil",
        "//java/arcs/core/storage/driver/volatiles",
        "//java/arcs/core/storage/keys",
        "//java/arcs/core/storage/referencemode",
        "//java/arcs/core/storage/testutil",  # buildcleaner: keep
        "//java/arcs/core/storage/testutil:fake_driver_provider",  # buildcleaner: keep
        "//java/arcs/core/testutil",
        "//java/arcs/core/type",
        "//java/arcs/core/util",
        "//java/arcs/core/util:utils-platform-dependencies",
        "//java/arcs/core/util/testutil",
        "//java/arcs/flags",
        "//java/arcs/flags/testing",
        "//java/arcs/jvm/storage/database/testutil",
        "//java/arcs/jvm/util",
        "//java/arcs/jvm/util/testutil",
        "//third_party/java/junit:junit-android",
        "//third_party/java/mockito:mockito-android",
        "//third_party/java/truth:truth-android",
        "//third_party/kotlin/kotlin:kotlin_test",
        "//third_party/kotlin/kotlinx_atomicfu",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/kotlinx_coroutines:kotlinx_coroutines_test",
        "//third_party/kotlin/mockito_kotlin:mockito_kotlin-android",
    ],
)

arcs_kt_jvm_library(
    name = "fakes",
    srcs = glob(
        ["*.kt"],
        exclude = TEST_SRCS,
    ),
    deps = [
        "//java/arcs/core/crdt",
        "//java/arcs/core/storage",
        "//java/arcs/core/util",
        "//third_party/kotlin/kotlinx_coroutines",
    ],
)
