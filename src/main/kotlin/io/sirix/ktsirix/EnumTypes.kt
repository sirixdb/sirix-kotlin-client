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

enum class NodeType(val value: String) {
    OBJECT("OBJECT"),
    ARRAY("ARRAY"),
    OBJECT_KEY("OBJECT_KEY"),
    OBJECT_STRING_VALUE("OBJECT_STRING_VALUE"),
    STRING_VALUE("STRING_VALUE"),
    OBJECT_NUMBER_VALUE("OBJECT_NUMBER_VALUE"),
    NUMBER_VALUE("NUMBER_VALUE"),
    OBJECT_BOOLEAN_VALUE("OBJECT_BOOLEAN_VALUE"),
    BOOLEAN_VALUE("BOOLEAN_VALUE"),
    OBJECT_NULL_VALUE("OBJECT_NULL_VALUE"),
    NULL_VALUE("NULL_VALUE"),
}

enum class InsertPosition(val value: String) {
    AS_FIRST_CHILD("asFirstChild"),
    AS_LEFT_SIBLING("asLeftSibling"),
    AS_RIGHT_SIBLING("asRightSibling"),
    REPLACE("replace"),
}

enum class DataType(val value: String) {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    NULL("null"),
    JSON_FRAGMENT("jsonFragment"),
}
