# jsoup is annotated with JSpecify, which is a compile-only dependency and so is
# absent at R8 time. The annotations are irrelevant to shrinking — just silence them.
-dontwarn org.jspecify.annotations.**
