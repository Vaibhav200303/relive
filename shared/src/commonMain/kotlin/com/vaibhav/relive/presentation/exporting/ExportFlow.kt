package com.vaibhav.relive.presentation.exporting

import com.vaibhav.relive.domain.exporting.ExportOperationState

enum class ExportFlowStage { Setup, Processing, Result, Error }

enum class ExportBackBehavior { ExitExport, LeaveRunning, ReturnToSetup }

fun ExportOperationState.flowStage(): ExportFlowStage = when (this) {
    ExportOperationState.Idle -> ExportFlowStage.Setup
    is ExportOperationState.Preparing,
    is ExportOperationState.Working,
    -> ExportFlowStage.Processing
    is ExportOperationState.Ready -> ExportFlowStage.Result
    is ExportOperationState.Failed -> ExportFlowStage.Error
}

fun ExportOperationState.backBehavior(): ExportBackBehavior = when (this) {
    ExportOperationState.Idle -> ExportBackBehavior.ExitExport
    is ExportOperationState.Preparing,
    is ExportOperationState.Working,
    -> ExportBackBehavior.LeaveRunning
    is ExportOperationState.Ready,
    is ExportOperationState.Failed,
    -> ExportBackBehavior.ReturnToSetup
}
