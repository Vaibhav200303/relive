package com.vaibhav.relive.platform.exporting

enum class ExportCompletion { Ready, Failed }

interface ExportCompletionNotifier {
    suspend fun notify(completion: ExportCompletion)
}

object UnavailableExportCompletionNotifier : ExportCompletionNotifier {
    override suspend fun notify(completion: ExportCompletion) = Unit
}
