load(
    "//third_party/java/arcs/build_defs:build_defs.bzl",
    "arcs_kt_library",
)

licenses(["notice"])

package(default_visibility = ["//java/arcs:allowed-packages"])

arcs_kt_library(
    name = "keys",
    srcs = glob(["*.kt"]),
    deps = [
        "//java/arcs/core/common",
        "//java/arcs/core/data",
        "//java/arcs/core/storage",
        "//java/arcs/flags",
    ],
)
