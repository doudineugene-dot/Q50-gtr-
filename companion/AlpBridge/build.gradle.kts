// Единственная точка входа сборки телефонного компаньона Q50 GTR+.
//
// Это отдельный проект, не связанный со сборкой самого Q50 GTR+ (build.sh,
// API 9/10, ручная цепочка aapt/javac/d8): телефон — современный Samsung
// Galaxy S21 FE, и MediaProjection на API 10 не существует в принципе.
// Общее с DCU-частью — только формат пакетов, описанный в
// docs/ALP-MIRROR-PROTOCOL.md, а не код и не тулчейн.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
