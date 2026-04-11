package ru.chinesewithai.backend.storedfile.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.chinesewithai.backend.storedfile.application.exception.FileUploadRejectedException;
import ru.chinesewithai.backend.storedfile.application.exception.InvalidUploadSessionStateException;
import ru.chinesewithai.backend.storedfile.application.exception.StorageIOException;
import ru.chinesewithai.backend.storedfile.application.exception.UploadSessionNotFoundException;

@RestControllerAdvice(
        assignableTypes = {StoredFileController.class, ru.chinesewithai.backend.storedfile.infrastructure.progress.StoredFileSseController.class})
public class StoredFileApiExceptionHandler {

    @ExceptionHandler(UploadSessionNotFoundException.class)
    ProblemDetail handleNotFound(UploadSessionNotFoundException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setTitle("Upload session not found");
        return pd;
    }

    @ExceptionHandler(InvalidUploadSessionStateException.class)
    ProblemDetail handleBadState(InvalidUploadSessionStateException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("Invalid upload session state");
        return pd;
    }

    @ExceptionHandler(FileUploadRejectedException.class)
    ProblemDetail handleRejected(FileUploadRejectedException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Upload rejected");
        return pd;
    }

    @ExceptionHandler(StorageIOException.class)
    ProblemDetail handleStorage(StorageIOException e) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        pd.setTitle("Object storage error");
        return pd;
    }
}
