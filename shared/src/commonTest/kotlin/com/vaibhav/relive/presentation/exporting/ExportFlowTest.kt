package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.exporting.ExportFormat
import com.vaibhav.relive.domain.exporting.ExportOperationState
import com.vaibhav.relive.domain.exporting.ExportProgress
import com.vaibhav.relive.domain.exporting.ExportResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ExportFlowTest {
    @Test
    fun operationStatesChooseTheExpectedFullScreenStage() {
        val preparing = ExportOperationState.Preparing(ExportFormat.KeepsakePdf)
        val working = ExportOperationState.Working(
            ExportFormat.ReliveArchive,
            ExportProgress(1, 2, "Packing media"),
        )
        val ready = ExportOperationState.Ready(
            ExportResult("export.pdf", "export.pdf", "application/pdf", ExportFormat.KeepsakePdf),
        )

        assertEquals(ExportFlowStage.Setup, ExportOperationState.Idle.flowStage())
        assertEquals(ExportFlowStage.Processing, preparing.flowStage())
        assertEquals(ExportFlowStage.Processing, working.flowStage())
        assertEquals(ExportFlowStage.Result, ready.flowStage())
        assertEquals(ExportFlowStage.Error, ExportOperationState.Failed("failed").flowStage())
    }

    @Test
    fun backNeverFallsThroughToThePhoneHomeScreen() {
        val working = ExportOperationState.Working(
            ExportFormat.ReliveArchive,
            ExportProgress(1, 2, "Packing media"),
        )
        val ready = ExportOperationState.Ready(
            ExportResult("export.pdf", "export.pdf", "application/pdf", ExportFormat.KeepsakePdf),
        )

        assertEquals(ExportBackBehavior.ExitExport, ExportOperationState.Idle.backBehavior())
        assertEquals(
            ExportBackBehavior.CancelGeneration,
            ExportOperationState.Preparing(ExportFormat.KeepsakePdf).backBehavior(),
        )
        assertEquals(ExportBackBehavior.CancelGeneration, working.backBehavior())
        assertEquals(ExportBackBehavior.ReturnToSetup, ready.backBehavior())
        assertEquals(
            ExportBackBehavior.ReturnToSetup,
            ExportOperationState.Failed("failed").backBehavior(),
        )
    }
}
