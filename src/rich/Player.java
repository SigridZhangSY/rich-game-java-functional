package rich;

import rich.environment.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Player {

    private final int id;
    private Land current;
    private Status status;
    private double balance;
    private List<Land> lands;
    private List<Tool> tools;
    private int freeTurn;
    private int waitTurn;
    private Map map;
    private Dice dice;
    private int points;


    public Player(int id, Map map, Dice dice, double balance) {
        this.id = id;
        this.map = map;
        this.dice = dice;
        this.balance = balance;
        status = Status.END_TURN;
        lands = new ArrayList<>();
        tools = new ArrayList<>();
        freeTurn = 0;
        waitTurn = 0;
        current = map.getStartPoint();
    }

    public static Player createPlayerWithStart(int id, Map map, Dice dice, Land start) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, 0, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.status = Status.WAIT_COMMAND;
        return player;
    }

    public static Player createPlayerWithBalance(int id, Map map, Dice dice, Land start, double balance) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, balance, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.status = Status.WAIT_COMMAND;
        return player;
    }

    public static Player createPlayerWithEstate(int id, Map map, Dice dice, Land start, double balance, Land...lands) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, balance, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.lands.addAll(asList(lands));
        player.status = Status.WAIT_COMMAND;
        return player;
    }

    public static Player createPlayerFreeForFee(int id, Map map, Dice dice, Land start, double balance, int freeTurns) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, balance, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.freeTurn = freeTurns;
        player.status = Status.WAIT_COMMAND;
        return player;
    }

    public static Player createPlayerWithPoint(int id, Map map, Dice dice, Land start, int points) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, 0, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.points = points;
        player.status = Status.WAIT_COMMAND;
        return player;
    }

    public static Player createPlayerWithTool(int id, Map map, Dice dice, Land start, Tool... tools) {
        Player player = createPlayerSpecifiedStatus(id, map, dice, 0, Status.WAIT_COMMAND, 0, 0);
        player.current = start;
        player.tools.addAll(asList(tools));
        return player;
    }

    public static Player createPlayerSpecifiedStatus(int id, Map map, Dice dice, double balance, Status status, int waitTurn, int freeTurn) {
        Player player = new Player(id, map, dice, balance);
        player.status = status;
        player.waitTurn = waitTurn;
        player.freeTurn = freeTurn;

        return player;
    }

    public void roll() {
        current = map.move(current, dice.next(), this);

        if (current instanceof Estate) {
            if (((Estate)current).getOwner() == null || ((Estate)current).getOwner() == this)
                status = Status.WAIT_RESPONSE;
            else if (((Estate)current).getOwner() != this) {
                payFee(((Estate)current).getOwner());
            }
        }
        if (current instanceof Prison) {
            status = Status.END_TURN;
            waitTurn = 2;
        }
        if (current instanceof GiftHouse) {
            status = Status.WAIT_RESPONSE;
        }
        if (current instanceof ToolHouse) {
            if (points >= ToolHouse.LOW_LIMIT)
                status = Status.WAIT_RESPONSE;
            else
                status = Status.END_TURN;
        }
        if(current instanceof Mine) {
            points += ((Mine) current).getPoints();
            status = Status.END_TURN;
        }
        if(current instanceof MagicHouse)
            status = Status.END_TURN;
    }

    private void payFee(Player owner) {
        if (freeTurn <= 0 &&
                !(((Estate)current).getOwner().getCurrent() instanceof Hospital)&&
                !(((Estate)current).getOwner().getCurrent() instanceof Prison)) {
            double fee = ((Estate)current).getPrice() * ((Estate)current).getLevel().getTimes();
            if (balance >= fee) {
                balance -= fee;
                owner.gain(fee);
            } else {
                status = Status.END_GAME;
                return;
            }
        }
        status = Status.END_TURN;
    }

    public void gain(double sum) {
        balance += sum;
    }

    public Status getStatus() {
        return status;
    }

    public void sayNo() {
        if (((Estate)current).getOwner() == null || ((Estate)current).getOwner() == this)
            status = Status.END_TURN;
    }

    public void sayYes() {
        if (((Estate)current).getOwner() == null)
            buy();
        else if (((Estate)current).getOwner() == this)
            promote();
        status = Status.END_TURN;
    }

    private void buy() {
        if (balance >= ((Estate)current).getPrice()) {
            balance -= ((Estate)current).getPrice();
            ((Estate)current).buy(this);
            lands.add(current);
        }
    }

    private void promote() {
        if (balance >= ((Estate)current).getPrice()) {
            if (((Estate)current).promote())
                balance -= ((Estate)current).getPrice();
        }
    }

    public void addPoint(int points) {
        this.points += points;
    }

    public void haveFreeTurn(int freeTurn) {
        this.freeTurn = freeTurn;
    }

    public void selectGift(int i) {
        ((GiftHouse)current).getGift(i, this);
        status = Status.END_TURN;
    }

    public void buyTool(String response) {
        char[] choiceStr = response.toCharArray();
        if (choiceStr.length > 1)
            return;
        if((choiceStr[0] < '1' || choiceStr[0] > '9') && choiceStr[0] != 'F' )
            return;
        if (choiceStr[0] == 'F'){
            status = Status.END_TURN;
            return;
        }
        if(tools.size() == 10)
            return;
        Tool tool = ((ToolHouse)current).getTool(choiceStr[0] - '0');
        if (tool != null) {
            int toolPointPrice = tool.getPointPrice();
            if (points >= toolPointPrice) {
                tools.add(tool);
                points -= toolPointPrice;
            }
        }
        if(points < ToolHouse.LOW_LIMIT)
            status = Status.END_TURN;
    }

    public void goToHospital(){
        waitTurn = 3;
        status = Status.END_TURN;
    }

    public boolean useTool(Tool.Type type, int distance){
        for (Tool tool:tools){
            if(tool.getType() == type) {
                switch (tool.getType()){
                    case ROBOT:
                        map.removeTool(current);
                        tools.remove(tool);
                        return true;
                    case BLOCK:
                        if(map.setTool(current, distance, Tool.Type.BLOCK)) {
                            tools.remove(tool);
                            return true;
                        }
                        return false;
                    case BOMB:
                        if (map.setTool(current, distance, Tool.Type.BOMB)) {
                            tools.remove(tool);
                            return true;
                        }
                        return false;
                    default:
                        return false;
                }
            }
        }
        return false;
    }

    public boolean sell(int i) {
        Land land = map.sellEstate(this, i);
        if(land != null) {
            Estate estate = (Estate) land;
            balance += estate.getPrice() * (estate.getLevel().ordinal() + 1) * 2;
            lands.remove(estate);
            return true;
        }
        return false;
    }

    public boolean sellTool(Tool.Type type) {
        for(Tool tool : tools){
            if(tool.getType() == type){
                tools.remove(tool);
                points += tool.getPointPrice();
                return true;
            }
        }
        return false;
    }

    public boolean startTurn(){
        if(status == Status.END_TURN) {
            if (freeTurn > 0){
                freeTurn -- ;
            }
            if(waitTurn == 0) {
                status = Status.WAIT_COMMAND;
                return true;
            }
            else {
                waitTurn -- ;
                return false;
            }
        }
        return true;
    }

    public Land getCurrent() {
        return current;
    }

    public List<Land> getLands() {
        return lands;
    }

    public double getBalance() {
        return balance;
    }

    public int getWaitTurn() {
        return waitTurn;
    }

    public int getFreeTurn() {
        return freeTurn;
    }

    public int getPoints() {
        return points;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public int getId() {
        return id;
    }

    public enum Status {WAIT_TURN, WAIT_COMMAND, WAIT_RESPONSE, END_TURN, END_GAME,}
}
