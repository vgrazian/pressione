// Error handling utilities

export class AppError extends Error {
    constructor(message, code = 'UNKNOWN', details = null) {
        super(message)
        this.name = 'AppError'
        this.code = code
        this.details = details
    }
}

export class SyncError extends AppError {
    constructor(message, details = null) {
        super(message, 'SYNC_ERROR', details)
        this.name = 'SyncError'
    }
}

export class ValidationError extends AppError {
    constructor(message, field = null) {
        super(message, 'VALIDATION_ERROR', { field })
        this.name = 'ValidationError'
    }
}

export function formatUserError(error) {
    if (error instanceof ValidationError) {
        return error.message
    }
    if (error instanceof SyncError) {
        return 'Errore di sincronizzazione: ' + error.message
    }
    if (error instanceof AppError) {
        return error.message
    }
    if (error?.message) {
        return 'Errore: ' + error.message
    }
    return 'Si è verificato un errore imprevisto.'
}
