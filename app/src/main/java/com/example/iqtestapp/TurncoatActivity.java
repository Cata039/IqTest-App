package com.example.iqtestapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TurncoatActivity extends AppCompatActivity {
    // Cell states
    private static final int EMPTY = -1, WHITE = 0, BLACK = 1;
    // Who is human/computer
    private static final int HUMAN = BLACK, COMPUTER = WHITE;

    // --- Game state ---
    private int[][] board = new int[6][6];
    private boolean[][] mine = new boolean[6][6];
    private int[] chipsLeft = {7, 7};  // index 0=WHITE,1=BLACK
    private int currentPlayer = HUMAN;
    private int lastMovedRow = -1, lastMovedCol = -1;


    // --- UI & selection ---
    private GridLayout grid;
    private Chronometer chronometer;
    private int selectedRow = -1, selectedCol = -1;
    private Handler handler = new Handler();

    // --- NEW: store player info for DB insert ---
    private String    playerName;
    private int       playerAge;
    private String    playerGender;
    private DBHelper  dbHelper;

    private TextView tvBlackCount, tvWhiteCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turncoat);  // <<< your game‐board layout!

        // pull extras
        Intent in = getIntent();
        playerName   = in.getStringExtra("name");
        playerAge    = in.getIntExtra("age", 0);
        playerGender = in.getStringExtra("gender");
        dbHelper     = new DBHelper(this);

        // start timer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        // find your new views:
        tvBlackCount = findViewById(R.id.tv_black_count);
        tvWhiteCount = findViewById(R.id.tv_white_count);
        updateCounts();      // show "7" for both at start

        // now build the board…
        grid = findViewById(R.id.grid_board);
        initBoard();
        initMines();
        buildGrid();
    }

    private void updateCounts() {
        tvBlackCount.setText("Black: " + chipsLeft[BLACK]);
        tvWhiteCount.setText("White: " + chipsLeft[WHITE]);
    }


    /** 1) Clear the board array */
    private void initBoard() {
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 6; c++)
                board[r][c] = EMPTY;
    }

    /** 2) Randomly hide 3 mines in each row */
    private void initMines() {
        Random rnd = new Random();
        for (int r = 0; r < 6; r++) {
            int placed = 0;
            while (placed < 3) {
                int c = rnd.nextInt(6);
                if (!mine[r][c]) {
                    mine[r][c] = true;
                    placed++;
                }
            }
        }
    }

    /** 3) Build a 6×6 checkerboard of ImageButtons */
    private void buildGrid() {
        grid.removeAllViews();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                final int row = r, col = c;
                ImageButton btn = new ImageButton(this);
                int tile = ((r + c) % 2 == 0)
                        ? R.drawable.tile_light
                        : R.drawable.tile_dark;
                btn.setBackgroundResource(tile);

                GridLayout.Spec rs = GridLayout.spec(r, 1f);
                GridLayout.Spec cs = GridLayout.spec(c, 1f);
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(rs, cs);
                lp.width = 0; lp.height = 0; lp.setMargins(2,2,2,2);
                btn.setLayoutParams(lp);

                btn.setOnClickListener(v -> handleCellTap(row, col, btn));
                grid.addView(btn);
            }
        }
    }

    /** Handle HUMAN taps */
    private void handleCellTap(int r, int c, ImageButton btn) {
        if (currentPlayer != HUMAN) return;

        int front = (HUMAN==BLACK?0:5);
        if (chipsLeft[HUMAN]>0 && r==front && board[r][c]==EMPTY) {
            placePebble(r, c, btn);
            return;
        }
        if (selectedRow<0) {
            if (board[r][c]!=EMPTY && !(r==lastMovedRow && c==lastMovedCol)) {
                selectedRow=r; selectedCol=c;
                btn.setColorFilter(Color.YELLOW);
            }
        } else {
            if (board[r][c]==EMPTY && isNeighbor(selectedRow,selectedCol,r,c)) {
                ImageButton prev=(ImageButton)grid.getChildAt(selectedRow*6+selectedCol);
                prev.clearColorFilter();
                movePebble(selectedRow,selectedCol,r,c,btn);
                selectedRow=selectedCol=-1;
            }
        }
    }

    /** 8-direction adjacency */
    private boolean isNeighbor(int r1,int c1,int r2,int c2){
        return Math.max(Math.abs(r1-r2),Math.abs(c1-c2))==1;
    }

    /** Place (no bomb) */
    private void placePebble(int r, int c, ImageButton btn) {
        board[r][c] = currentPlayer;
        chipsLeft[currentPlayer]--;
        btn.setImageResource(currentPlayer == BLACK
                ? R.drawable.circle_black
                : R.drawable.circle_white);

        updateCounts();    // ← new

        if (checkGameOver()) return;
        endTurn(r, c);
    }


    /** Move (bomb flips) */
    private void movePebble(int r1,int c1,int r2,int c2,ImageButton dest){
        board[r1][c1]=EMPTY;
        ImageButton src=(ImageButton)grid.getChildAt(r1*6+c1);
        src.setImageDrawable(null);

        board[r2][c2]=currentPlayer;
        dest.setImageResource(currentPlayer==BLACK
                ? R.drawable.circle_black
                : R.drawable.circle_white);

        if (mine[r2][c2]) {
            board[r2][c2]=1-board[r2][c2];
            dest.setImageResource(board[r2][c2]==BLACK
                    ? R.drawable.circle_black
                    : R.drawable.circle_white);
        }
        if (checkGameOver()) return;
        endTurn(r2,c2);
    }

    /** End turn & schedule AI if needed */
    private void endTurn(int lr,int lc){
        lastMovedRow=lr; lastMovedCol=lc;
        currentPlayer=1-currentPlayer;
        if (currentPlayer==COMPUTER)
            handler.postDelayed(this::aiMove, 500);
    }

    /** Candidate action */
    private static class Action { int r1,c1,r2,c2;
        Action(int r,int c){ this(r,c,-1,-1); }
        Action(int r1,int c1,int r2,int c2){
            this.r1=r1; this.c1=c1; this.r2=r2; this.c2=c2;
        }
    }

    /** Smarter AI */
    private void aiMove(){
        List<Action> actions=new ArrayList<>();
        int front=(COMPUTER==BLACK?0:5);

        // placements
        if (chipsLeft[COMPUTER]>0){
            for(int c=0;c<6;c++)
                if(board[front][c]==EMPTY)
                    actions.add(new Action(front,c));
        }
        // moves
        for(int r=0;r<6;r++){
            for(int c=0;c<6;c++){
                if(board[r][c]!=EMPTY && !(r==lastMovedRow&&c==lastMovedCol)){
                    for(int dr=-1;dr<=1;dr++){
                        for(int dc=-1;dc<=1;dc++){
                            if(dr==0&&dc==0) continue;
                            int rr=r+dr, cc=c+dc;
                            if(rr>=0&&rr<6&&cc>=0&&cc<6&&board[rr][cc]==EMPTY)
                                actions.add(new Action(r,c,rr,cc));
                        }
                    }
                }
            }
        }

        int[][] tmpB=new int[6][6];
        boolean[][] tmpM=new boolean[6][6];
        Action best=null;
        double bestScore=-1e9;

        for(Action a:actions){
            copyBoard(board,tmpB);
            copyBoard(mine,tmpM);
            int[] tmpC=chipsLeft.clone();

            simulate(a,tmpB,tmpM,tmpC,COMPUTER);

            if(checkWin(tmpB,COMPUTER)){ best=a; break; }
            if(letsHumanWin(tmpB,tmpC)) continue;

            // — CHANGED HEURISTIC —
            int whiteChain = longestChain(tmpB, COMPUTER);
            int blackChain = longestChain(tmpB, HUMAN);
            double sc = whiteChain - blackChain;
            if(sc>bestScore){ bestScore=sc; best=a; }
        }

        if(best!=null){
            if(best.r2<0){
                ImageButton btn=(ImageButton)grid.getChildAt(best.r1*6+best.c1);
                placePebble(best.r1,best.c1,btn);
            } else {
                ImageButton dest=(ImageButton)grid.getChildAt(best.r2*6+best.c2);
                movePebble(best.r1,best.c1,best.r2,best.c2,dest);
            }
        } else {
            handler.post(this::aiMove);
        }
    }

    private void copyBoard(int[][] s,int[][] d){
        for(int i=0;i<6;i++) System.arraycopy(s[i],0,d[i],0,6);
    }
    private void copyBoard(boolean[][] s,boolean[][] d){
        for(int i=0;i<6;i++) System.arraycopy(s[i],0,d[i],0,6);
    }

    private void simulate(Action a,int[][] b,boolean[][] m,int[] cp,int player){
        if(a.r2<0){
            b[a.r1][a.c1]=player; cp[player]--;
        } else {
            int col=b[a.r1][a.c1];
            b[a.r1][a.c1]=EMPTY;
            b[a.r2][a.c2]=col;
            if(m[a.r2][a.c2]) b[a.r2][a.c2]=1-b[a.r2][a.c2];
        }
    }

    private boolean letsHumanWin(int[][] b,int[] cp){
        int front=(HUMAN==BLACK?0:5);
        if(cp[HUMAN]>0){
            for(int c=0;c<6;c++){
                if(b[front][c]==EMPTY){
                    b[front][c]=HUMAN;
                    if(checkWin(b,HUMAN)){
                        b[front][c]=EMPTY;
                        return true;
                    }
                    b[front][c]=EMPTY;
                }
            }
        }
        return false;
    }

    private int longestChain(int[][] b,int color){
        int max=0;
        for(int r=0;r<6;r++) for(int c=0;c<6;c++){
            if(b[r][c]!=color) continue;
            max=Math.max(max,countDir(r,c,1,0,color,b));
            max=Math.max(max,countDir(r,c,0,1,color,b));
            max=Math.max(max,countDir(r,c,1,1,color,b));
            max=Math.max(max,countDir(r,c,1,-1,color,b));
        }
        return max;
    }

    private int countDir(int r,int c,int dr,int dc,int color,int[][] b){
        int cnt=0;
        while(r>=0&&r<6&&c>=0&&c<6&&b[r][c]==color){
            cnt++; r+=dr; c+=dc;
        }
        return cnt;
    }

    private boolean checkGameOver(){
        if(checkWin(board,HUMAN)){ announceWinner(HUMAN); return true; }
        if(checkWin(board,COMPUTER)){ announceWinner(COMPUTER); return true; }
        return false;
    }

    private boolean checkWin(int[][] b,int color){
        for(int r=0;r<6;r++) for(int c=0;c<6;c++){
            if(b[r][c]!=color) continue;
            if(countDir(r,c,1,0,color,b)>=4) return true;
            if(countDir(r,c,0,1,color,b)>=4) return true;
            if(countDir(r,c,1,1,color,b)>=4) return true;
            if(countDir(r,c,1,-1,color,b)>=4) return true;
        }
        return false;
    }

    /** Stops timer & shows elapsed time */
    private void announceWinner(int winner) {
        chronometer.stop();
        long secs = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
        String who   = (winner == BLACK ? "Black" : "White");
        String result = (winner == HUMAN ? "won" : "lost");

        // Insert into DB
        dbHelper.insertResult(
                playerName,
                playerAge,
                playerGender,
                "Turncoat",
                secs,
                result
        );

        // Inflate our custom dialog view
        // inside your announceWinner() in TurncoatActivity:
        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_game_over, null, false);

        TextView tv = dialogView.findViewById(R.id.tv_dialog_message);
        tv.setText(who + " wins in " + secs + " seconds!");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btn_next).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, MazeQuizInstructionsActivity.class);
            intent.putExtra("name", playerName);
            intent.putExtra("age", playerAge);
            intent.putExtra("gender", playerGender);
            startActivity(intent);
            finish();
        });

        dialog.show();

    }

}