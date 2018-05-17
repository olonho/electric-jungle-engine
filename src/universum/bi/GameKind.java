package universum.bi;

/**
 * Kind of the game.
 *
 * @see UserGameInfo
 * @author nike
 */
public enum GameKind {
    /**
     * Single player game
     */
    SINGLE,
    /**
     * Two players, attacks allowed
     */
    DUEL,
    /**
     * Two players, no attacks
     */
    PEACE_DUEL,
    /**
     * Up to 8 players
     */
    JUNGLE,
    /**
     * Debugging mode
     */
    DEBUG
};
