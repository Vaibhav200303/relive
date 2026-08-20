package com.vaibhav.relive.data

/**
 * Raised when a persisted row contains a value that cannot be mapped to a valid
 * domain representation (unknown enum name, blank id, malformed coordinates,
 * ...). Mapping fails loudly rather than silently substituting a default so that
 * corrupt persistence is visible immediately.
 */
class PersistenceMappingException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
