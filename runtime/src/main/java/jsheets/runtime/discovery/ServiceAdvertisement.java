package jsheets.runtime.discovery;

/**
 * Represents an existing entry in a service discovery backend.
 */
public interface ServiceAdvertisement {
  /**
   * Deletes the entry from the discovery backend.
   */
  void remove();
}
