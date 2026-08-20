package com.vaibhav.relive.debug

import com.vaibhav.relive.domain.repository.MomentRepository

/**
 * Release variant: no debug override; the composition root uses the real
 * SQLDelight-backed repository. Kept as a variant-swapped source-set file so
 * the debug sample repository is never compiled into a production build.
 */
fun debugMomentRepositoryOverrideOrNull(): MomentRepository? = null
