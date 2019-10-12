package com.group6.mdp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GridMap extends View {

    private static final String TAG = "GridMap";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;

    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject mapInformation;
    private static JSONObject backupMapInformation;

    private static Cell[][] cells;
    private static String robotDirection = "None";

    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};

    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();

    private static HashMap<Integer, HashMap<Integer, Integer>> numberBlocksCoord = new HashMap<>();

    private static boolean autoUpdate = false;
    private static boolean mapDrawn = false;
    private static boolean canDrawRobot = false;

    private static boolean setWaypointStatus = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;

    private static boolean validPosition = false;
    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

    private Paint blackPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint waypointColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint arrowColor = new Paint();
    private Paint fastestPathColor = new Paint();
    private Paint numberBlockColor = new Paint();
    private Paint numberBlockTextColor = new Paint();

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public GridMap(Context context) {
        super(context);
        init(null);
    }

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.GREEN);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.YELLOW);
        unexploredColor.setColor(Color.GRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);
        fastestPathColor.setColor(Color.MAGENTA);
        numberBlockColor.setARGB(255,255,165,0);
        numberBlockTextColor.setColor(Color.BLACK);
        numberBlockTextColor.setTextAlign(Paint.Align.CENTER);
        numberBlockTextColor.setTextSize(15);
    }

    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        printLog("Entering onDraw");
        super.onDraw(canvas);
        printLog("Redrawing map");

        ArrayList<String[]> arrowCoord = this.getArrowCoord();
        int[] curCoord = this.getCurCoord();

        if (!this.getMapDrawn()) {
            //canvas.drawColor(Color.parseColor("#D4AF37"));
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            this.createCell();
            this.setEndCoord(14, 19);
            mapDrawn = true;
        }

        this.drawIndividualCell(canvas);
        this.drawGridNumber(canvas);

        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        this.drawArrow(canvas, arrowCoord);

        printLog("Exiting onDraw");
    }

    private void createCell() {
        printLog("Entering cellCreate");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        printLog("Exiting createCell");
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        printLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getMapDrawn() {
        return mapDrawn;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    public void setEndCoord(int col, int row) {
        printLog("Entering setEndCoord");
        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        printLog("Exiting setEndCoord");
    }

    public void setStartCoord(int col, int row) {
        printLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;

        if (this.getStartCoordStatus()){
            this.setCurCoord(col, row, "right");
        }
        printLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        printLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        printLog("Exiting setCurCoord");
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.x_pos);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.y_pos);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.direction);

        xAxisTextView.setText(String.valueOf(col));
        yAxisTextView.setText(String.valueOf(row));
        directionAxisTextView.setText(String.format("DIRECTION: %s", direction));
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    public void setRobotDirection(String direction) {
        Log.i(TAG, String.format("Set Direction To %s", direction));
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    private void setWaypointCoord(int col, int row) throws JSONException {
        printLog("Entering setWaypointCoord");
        waypointCoord[0] = col;
        waypointCoord[1] = row;

        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        MainActivity.sendMessage("waypoint", waypointCoord[0], waypointCoord[1]);
        printLog("Exiting setWaypointCoord");
    }

    private int[] getWaypointCoord() {
        return waypointCoord;
    }

    private void setObstacleCoord(int col, int row) {
        printLog("Entering setObstacleCoord");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        printLog("Obstacle Coordinate set at x: " + col + " row: " + row);
        printLog("Exiting setObstacleCoord");
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    private void setNumberBlockCoord(int col, int row, int index){
        printLog("Entering setNumberBlockCoord");
        row = this.convertRow(row);
        HashMap<Integer, Integer> hashmap =  new HashMap<>();

        if(GridMap.numberBlocksCoord.containsKey(col)){
            hashmap = GridMap.numberBlocksCoord.get(col);

            if(GridMap.numberBlocksCoord.get(col).containsKey(row))
                hashmap.replace(row, index);
            else
                hashmap.put(row, index);
        }
        else
            hashmap.put(row, index);


        GridMap.numberBlocksCoord.put(col,hashmap);
        cells[col][row].setType("numberBlock");
        printLog("Exiting setNumberBlockCoord");
    }

    private HashMap<Integer, HashMap<Integer, Integer>> getNumberBlockCoord(){
        return numberBlocksCoord;
    }

    public void moveRobot(String direction) {
        printLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        printLog("Exiting moveRobot");
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        printLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        printLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        printLog("Entering setArrowCoordinate");
        int[] obstacleCoord = new int[]{col, row};
        this.getObstacleCoord().add(obstacleCoord);
        String[] arrowCoord = new String[3];
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;
        this.getArrowCoord().add(arrowCoord);

        row = convertRow(row);
        cells[col][row].setType("arrow");
        printLog("Exiting setArrowCoordinate");
    }

    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    private void drawIndividualCell(Canvas canvas) {
        printLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++){
                for (int i = 0; i < this.getArrowCoord().size(); i++){
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                }
                if(cells[x][y].type.equals("numberBlock")){
                    try{
                        int result = numberBlocksCoord.get(x).get(y).intValue();
                        Log.d(TAG, "numberBlocks: " + numberBlocksCoord.toString());
                        float x_pos = cells[x][y].startX + (cellSize / 2);
                        float y_pos = cells[x][y].startY + cellSize / 1.5f;
                        canvas.drawText(String.format("%d", result), x_pos, y_pos, numberBlockTextColor);
                    }
                    catch(NullPointerException e){
                        Log.e(TAG, "Error writing text to cell: " + e.getMessage());
                    }
                }
            }

        printLog("Exiting drawIndividualCell");
    }

    private void drawGridNumber(Canvas canvas) {
        printLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), blackPaint);
        }
        for (int y = 0; y < ROW; y++) {
            if ((20 - y) > 9)
                canvas.drawText(Integer.toString(20 - y), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            else
                canvas.drawText(Integer.toString(20 - y), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
        }
        printLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        printLog("Entering drawRobot");
        int androidRowCoord = this.convertRow(curCoord[1]);
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);

        switch (this.getRobotDirection().toLowerCase()) {
            case "up":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "down":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;
            case "right":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;
            case "left":
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;
            default:
                Utils.showToast(this.getContext(), "Error with drawing robot (unknown direction)");
                break;
        }
    }

    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        printLog("Entering drawArrow");
        RectF rect;

        for (int i = 0; i < arrowCoord.size(); i++) {
            if (!arrowCoord.get(i)[2].equals("dummy")) {
                int col = Integer.parseInt(arrowCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoord.get(i)[2]) {
                    case "up":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_up);
                        break;
                    case "right":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_right);
                        break;
                    case "down":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_down);
                        break;
                    case "left":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_left);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            printLog("Exiting drawArrow");
        }
    }

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                case "numberBlock":
                    this.paint = numberBlockColor;
                    break;
                default:
                    printLog("setting default to: " + type);
                    break;
            }
        }
    }

    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    public void refreshMap() {
        if (this.getAutoUpdate())
            postInvalidateDelayed(500);
    }

    //This method sets the JSONObject used by updateMapInformation()
    // to the initializing robot position JSONObject and runs updateMapInformation()
    public void robotMessageForUpdateMapInformation(int x_pos, int y_pos, String direction)throws  JSONException{
        JSONObject sendObject = new JSONObject();
        JSONObject moveRobot = new JSONObject();
        JSONArray  arr = new JSONArray();

        moveRobot.put("x", x_pos+1);
        moveRobot.put("y", y_pos+1);
        moveRobot.put("direction", direction);
        moveRobot.put("move", true);

        arr.put(moveRobot);
        sendObject.put("robot", arr);

        setReceivedJsonObject(sendObject);
        updateMapInformation();
    }

    public void updateMapInformation() throws JSONException {

        printLog("Start of updateMapInformation()");
        JSONObject mapInformation = this.getReceivedJsonObject();
        printLog("updateMapInformation() mapInformation: " + mapInformation);

        JSONArray infoJsonArray;
        JSONObject infoJsonObject;

        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;

        if (mapInformation == null)
            return;

        for(int i=0; i<mapInformation.names().length(); i++) {
            message = "Print default message for updateMapInformation()";
            switch (mapInformation.names().getString(i)) {
                case "map":
                    infoJsonArray = mapInformation.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    hexStringExplored = infoJsonObject.getString("explored");

                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    exploredString = exploredString.substring(2, exploredString.length()-2);

                    printLog("updateMapInformation exploredString: " + exploredString);

                    int x, y;

                    for (int j=0; j<exploredString.length(); j++) {
                        y = 19 - (j/15);
                        x = 1 + j - ((19-y)*15);

                        if ((String.valueOf(exploredString.charAt(j))).equals("1") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("explored");
                        else if ((String.valueOf(exploredString.charAt(j))).equals("0") && !cells[x][y].type.equals("robot"))
                            cells[x][y].setType("unexplored");
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    printLog("updateMapInformation hexStringObstacle: " + hexStringObstacle);

                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    printLog("updateMapInformation hexBigIntegerObstacle: " + hexBigIntegerObstacle);

                    obstacleString = hexBigIntegerObstacle.toString(2);

                    while (obstacleString.length() < length)
                        obstacleString = "0" + obstacleString;

                    printLog("updateMapInformation obstacleString: " + obstacleString);

                    int k = 0;
                    for (int row = ROW-1; row >= 0; row--){
                        for (int col = 1; col <= COL; col++){
                            if ((cells[col][row].type.equals("explored")||(cells[col][row].type.equals("robot"))) && k < obstacleString.length()) {

                                String charAt = String.valueOf(obstacleString.charAt(k));
                                Log.i(TAG, String.format("k at: %s charAt:%s row: %s col: %s ", k, charAt, row, col));

                                if (charAt.equals("1"))
                                    setObstacleCoord(col, 20 - row);
                            }
                            k++;
                        }

                    }


                    int[] waypointCoord = this.getWaypointCoord();
                    if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                        cells[waypointCoord[0]][20-waypointCoord[1]].setType("waypoint");

                    String[] coordinate = infoJsonObject.getString("coordinate").replace("[", "").replace("]", "").split(",");
                    String direction = infoJsonObject.getString("direction").toLowerCase();

                    int x_pos = Integer.parseInt(coordinate[1].trim());
                    int y_pos =  Integer.parseInt(coordinate[0].trim());

                    robotMessageForUpdateMapInformation(x_pos, y_pos, direction);

                    break;
                case "robot":
                    if (canDrawRobot)
                        setOldRobotCoord(curCoord[0], curCoord[1]);
                    infoJsonArray = mapInformation.getJSONArray("robot");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    if(!infoJsonObject.has("move")){
                        for (int row = ROW-1; row >= 0; row--){
                            for (int col = 1; col <= COL; col++){
                                cells[col][row].setType("unexplored");
                            }
                        }
                        setEndCoord(14, 19);
                        setStartCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    }

                    setCurCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("direction"));
                    canDrawRobot = true;
                    break;
                case "waypoint":
                    infoJsonArray = mapInformation.getJSONArray("waypoint");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    this.setWaypointCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    setWaypointStatus = true;
                    break;
                case "obstacle":
                    infoJsonArray = mapInformation.getJSONArray("obstacle");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        this.setObstacleCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    }
                    message = "No. of Obstacle: " + String.valueOf(infoJsonArray.length());
                    break;
                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("dummy")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  (" + String.valueOf(infoJsonObject.getInt("x")) + "," + String.valueOf(infoJsonObject.getInt("y")) + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;
                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    String move = infoJsonObject.getString("direction");
                    Log.i(TAG, "Moving " + move);
                    if (canDrawRobot)
                        moveRobot(move);
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    break;
                case "status":
                    infoJsonArray = mapInformation.getJSONArray("status");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    printRobotStatus(infoJsonObject.getString("status"));
                    message = "status: " + infoJsonObject.getString("status");
                    break;
                case "numberBlock":
                    infoJsonArray = mapInformation.getJSONArray("numberBlock");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    int x_coord = infoJsonObject.getInt("x");
                    int y_coord = infoJsonObject.getInt("y");
                    int index = infoJsonObject.getInt("index");
                    if(index < 1 || index > 15){
                        Utils.showToast(getContext(), "Invalid Index Input");
                        break;
                    }

                    if(cells[x_coord][20 - y_coord].type == "obstacle" || cells[x_coord][20 - y_coord].type == "numberBlock")
                        this.setNumberBlockCoord(x_coord, y_coord, infoJsonObject.getInt("index"));
                    else
                        Log.e(TAG, "Error: Specified Coordinates Does Not Belong To An Obstacle Block nor a Number Block");
                    break;
                default:
                    message = "Unintended default for JSONObject";
                    break;
            }

            if (!message.equals("updateMapInformation Default message"))
                MainActivity.receiveMessage(message);
        }
        printLog("Exiting updateMapInformation");
        this.invalidate();
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        printLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    public JSONObject getMapInformation() {
        printLog("getCreateJsonObject() :" + getCreateJsonObject());
        return this.getCreateJsonObject();}

    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotstatus);
        robotStatusTextView.setText(message);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        printLog("Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize));
            ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.setrobotstartpoint);
            ToggleButton setWaypointToggleBtn = ((Activity)this. getContext()).findViewById(R.id.setwaypoint);

            if (startCoordStatus)
            {
                if (canDrawRobot)
                {
                    int[] startCoord = this.getStartCoord();

                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                }
                else
                    canDrawRobot = true;
                this.setStartCoord(column, row);
                startCoordStatus = false;

                try {
                    MainActivity.sendMessage("starting", column, row);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }

                updateRobotAxis(column, row, "up");

                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();

                this.invalidate();

                return true;
            }
            if (setWaypointStatus) {
               int[] waypointCoord = this.getWaypointCoord();
               if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                   cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                setWaypointStatus = false;
                try {
                   this.setWaypointCoord(column, row);
               } catch (JSONException e) {
                   e.printStackTrace();
               }
                if (setWaypointToggleBtn.isChecked())
                   setWaypointToggleBtn.toggle();
               this.invalidate();
               return true;
            }
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            if (setExploredStatus) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
                        obstacleCoord.remove(i);
                this.invalidate();
                return true;
            }

            this.invalidate();
            return true;
        }
        else if(this.getAutoUpdate()){
            Utils.showToast(getContext(), "Please set to Manual Mode.");
        }
        printLog("Exiting onTouchEvent");
        return false;
    }

    public JSONObject getCreateJsonObject() {
        printLog("Entering getCreateJsonObject");
        String exploredString = "11";
        String obstacleString = "";
        String hexStringObstacle = "";
        String hexStringExplored = "";
        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;
        int[] waypointCoord = this.getWaypointCoord();
        int[] curCoord = this.getCurCoord();
        String robotDirection = this.getRobotDirection();
        List<int[]> obstacleCoord = new ArrayList<>(this.getObstacleCoord());
        List<String[]> arrowCoord = new ArrayList<>(this.getArrowCoord());

        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotstatus);

        JSONObject map = new JSONObject();
        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";
        printLog("exploredString: " + exploredString);

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        printLog("hexBigIntegerExplored: " + hexBigIntegerExplored);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        printLog("hexStringExplored: " + hexStringExplored);

        for (int y=ROW-1; y>=0; y--)
            for (int x=1; x<=COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("arrow"))
                    obstacleString = obstacleString + "1";
        printLog("Before loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        printLog("After loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        if (!obstacleString.equals("")) {
            hexBigIntegerObstacle = new BigInteger(obstacleString, 2);
            printLog("hexBigIntegerObstacle: " + hexBigIntegerObstacle);
            hexStringObstacle = hexBigIntegerObstacle.toString(16);
            if (hexStringObstacle.length() % 2 != 0)
                hexStringObstacle = "0" + hexStringObstacle;
            printLog("hexStringObstacle: " + hexStringObstacle);
        }
        try {
            map.put("explored", hexStringExplored);
            map.put("length", obstacleString.length());
            if (!obstacleString.equals(""))
                map.put("obstacle", hexStringObstacle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonMap = new JSONArray();
        jsonMap.put(map);

        JSONArray jsonRobot = new JSONArray();
        if (curCoord[0] >= 2 && curCoord[1] >= 2)
            try {
                JSONObject robot = new JSONObject();
                robot.put("x", curCoord[0]);
                robot.put("y", curCoord[1]);
                robot.put("direction", robotDirection);
                jsonRobot.put(robot);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonWaypoint = new JSONArray();
        if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
            try {
                JSONObject waypoint = new JSONObject();
                waypoint.put("x", waypointCoord[0]);
                waypoint.put("y", waypointCoord[1]);
                setWaypointStatus = true;
                jsonWaypoint.put(waypoint);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonObstacle = new JSONArray();
        for (int i=0; i<obstacleCoord.size(); i++)
            try {
                JSONObject obstacle = new JSONObject();
                obstacle.put("x", obstacleCoord.get(i)[0]);
                obstacle.put("y", obstacleCoord.get(i)[1]);
                jsonObstacle.put(obstacle);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        JSONArray jsonArrow = new JSONArray();
        for (int i=0; i<arrowCoord.size(); i++) {
            try {
                JSONObject arrow = new JSONObject();
                arrow.put("x", Integer.parseInt(arrowCoord.get(i)[0]));
                arrow.put("y", Integer.parseInt(arrowCoord.get(i)[1]));
                arrow.put("face", arrowCoord.get(i)[2]);
                jsonArrow.put(arrow);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray jsonStatus = new JSONArray();
        try {
            JSONObject status = new JSONObject();
            status.put("status", robotStatusTextView.getText().toString());
            jsonStatus.put(status);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mapInformation = new JSONObject();
        try {
            mapInformation.put("map", jsonMap);
            mapInformation.put("robot", jsonRobot);
            if (setWaypointStatus) {
                mapInformation.put("waypoint", jsonWaypoint);
                setWaypointStatus = false;
            }
            mapInformation.put("obstacle", jsonObstacle);
            mapInformation.put("arrow", jsonArrow);
            mapInformation.put("status", jsonStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        printLog("Exiting getCreateJsonObject");
        return mapInformation;
    }

    public void resetMap() {
        printLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotstatus);
        ToggleButton modeToggleBtn = ((Activity)this.getContext()).findViewById(R.id.modeToggleButton);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("NA");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("direction", "None");
        editor.commit();

        receivedJsonObject = null;
        backupMapInformation = null;

        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};

        robotDirection = "None";
        autoUpdate = false;

        arrowCoord = new ArrayList<>();
        obstacleCoord = new ArrayList<>();
        numberBlocksCoord.clear();

        waypointCoord = new int[]{-1, -1};
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_error);

        printLog("Exiting resetMap");
        this.invalidate();
    }

    private void sharedPreferences() {
        sharedPreferences = this.getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void printLog(String message) {
        Log.d(TAG, message);
    }
}