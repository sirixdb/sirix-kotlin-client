package io.sirix.ktsirix

enum class DbType(val value: String) {
    XML("application/xml"),
    JSON("application/json")
}

enum class MetadataType(val value: String) {
    ALL("True"),
    KEY("nodeKey"),
    KEY_AND_CHILD("nodeKeyAndChildCount")
}

enum class Insert(val value: String) {
    CHILD("asFirstChild"),
    LEFT("asLeftSibling"),
    RIGHT("asRightSibling"),
    REPLACE("replace")
}
