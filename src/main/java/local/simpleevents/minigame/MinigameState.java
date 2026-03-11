package local.simpleevents.minigame;

/** The lifecycle state of a minigame session. */
public enum MinigameState {
    /** Accepting registrations but not yet started. */
    WAITING,
    /** Countdown before game starts. */
    STARTING,
    /** Game is in progress. */
    IN_PROGRESS,
    /** Game has ended, cleanup in progress. */
    ENDED
}
