package io.sirix.ktsirix

import java.io.IOException

class SirixHttpClientException(msg: String, cause: Throwable? = null) : IOException(msg, cause)
