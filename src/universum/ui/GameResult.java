package universum.ui;

import universum.engine.ScoreData;
import universum.engine.GameInfo;
import universum.bi.GameKind;
import java.util.*;
import java.io.*;

public final class GameResult {
    private List<ScoreData> results;
    private int numTurns;
    private GameKind gameKind;

    public GameResult(universum.engine.RenderingInfo ri) {
        init(ri.scores(), ri.numTurns(), ri.getGameKind());
    }

    public GameResult(Map<String,ScoreData> scores, 
                      int numTurns, GameKind gameKind) {
        init(scores, numTurns, gameKind);
    }

    private void init(Map<String,ScoreData> scores, 
                      int numTurns, GameKind gameKind) {
        results = new ArrayList<ScoreData>(scores.values());
        Collections.sort(results);
        this.numTurns = numTurns; 
        this.gameKind = gameKind;
    }
    

    public ScoreData winner() {
        if (results.size() > 0) {
            ScoreData rv = results.get(0);
            return rv.alive ? rv : null;
        }
        return null;
    }

    public List<ScoreData> results() {
        return this.results;
    }

    public String toString() {       
        StringBuffer result = new StringBuffer();
        result.append("gameKind: "+gameKind+"\n");
        result.append("numTurns: "+numTurns+"\n");        
        for (ScoreData sd: results) {            
            result.append(sd.player+" with id="+sd.id);
            result.append(":    jar="+sd.jarFile);
            result.append(":    score="+sd.score);
            result.append("     energy="+sd.energy);
            result.append("\n");
            String e = sd.getErrors();
            if (e.length() > 0) {
                result.append("Errors:\n");
                result.append(e);
            }
        }
        return result.toString();
    }

    public int numTurns() {
        return this.numTurns;
    }

    public GameKind kind() {
        return this.gameKind;
    }


    static HashMap<String, Integer> kindsMap;
    static {
        kindsMap = new HashMap<String, Integer>();
        kindsMap.put("SINGLE", 1);
        kindsMap.put("DUEL",   2);
        kindsMap.put("JUNGLE", 3);
    }

    public void storeAsSql(String file, GameInfo gi) throws IOException {        
        PrintStream ps = new PrintStream(file);
        String TAG="_FINAL"+kind();

        ps.println("INSERT INTO contests"+TAG+
                   " (owner, begin, end, took, turns, kind, host, dead, save)"+
                   " VALUES " +
                   " (0,'2007-03-05 20:50:30','2007-03-05 20:50:31',1000,"
                   + numTurns+","+kindsMap.get(kind().toString())+","+"'localhost'"+
                   " ,0, '"+gi.recordGameFilePath+"');");
        StringBuffer res = new StringBuffer("INSERT INTO results"+TAG+
                                            " (contest, being, score, victory)"+
                                            " VALUES ");
        ScoreData winner = winner();
        for (Iterator<ScoreData> i = results.iterator(); i.hasNext(); ) {
            ScoreData sd = i.next();
            int under = sd.jarFile.indexOf('_');
            if (under == -1) {
                throw new RuntimeException("bad jar: "+sd.jarFile);
            }
            String user = sd.jarFile.substring(0, under);
            String jar = sd.jarFile.substring(under+1);
            
            res.append("(LAST_INSERT_ID(), (SELECT id FROM beings WHERE jarfile='"+
                       jar+"' AND owner=(SELECT id FROM users WHERE nick='"+
                       user+"') ),"+sd.score+","+((sd == winner) ? 1 : 0)+")");
            res.append(i.hasNext() ? ',' : ';');
        }
        ps.println(res.toString());
        ps.close();
    } 
}
