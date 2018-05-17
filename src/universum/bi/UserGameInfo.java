package universum.bi;

/**
 * Pure data class containing info about game visible to user,
 * passed to reinit() metod of Being
 *
 * @see Being#reinit(UserGameInfo)
 */
public class UserGameInfo {
    /**
     * kind of the game
     */
    public GameKind kind;
    /**
     * number of turns
     */
    public int maxTurns;

    public UserGameInfo() {
    }
}
