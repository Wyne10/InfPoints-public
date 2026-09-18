package org.bigcraft.infpoints.storage;

public class StorageUnavailableException extends StorageException {

    public StorageUnavailableException(String message) {
        super(message);
    }

    public StorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

}
