load(
    "//third_party/java/arcs/build_defs:build_defs.bzl",
    "arcs_kt_gen",
    "arcs_kt_jvm_test_suite",
)

licenses(["notice"])

package(default_visibility = ["//java/arcs:allowed-packages"])

arcs_kt_gen(
    name = "codegen",
    srcs = ["EvaluatorParticleTest.arcs"],
    test_harness = True,
)

arcs_kt_jvm_test_suite(
    name = "expression",
    flaky = True,
    package = "arcs.core.data.expression",
    deps = [
        ":codegen",
        ":codegen_schema_test_harness",
        "//java/arcs/core/analytics",
        "//java/arcs/core/data",
        "//java/arcs/core/data/expression",
        "//java/arcs/core/data/expression:particle",
        "//java/arcs/core/entity",
        "//java/arcs/core/testutil",
        "//java/arcs/core/testutil/handles",
        "//java/arcs/core/util",
        "//java/arcs/core/util:utils-platform-dependencies",
        "//java/arcs/core/util/testutil",
        "//java/arcs/jvm/util/testutil",
        "//java/arcs/sdk/testing",
        "//third_party/java/junit",
        "//third_party/java/mockito",
        "//third_party/java/truth",
        "//third_party/kotlin/kotlin:kotlin_test",
        "//third_party/kotlin/kotlinx_coroutines",
        "//third_party/kotlin/mockito_kotlin",
    ],
)
