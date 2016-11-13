package rich.player;

import org.junit.Before;
import org.junit.Test;
import rich.Player;
import rich.environment.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerSellToolTest {
    private Map map;
    private Dice dice;
    private Land startPoint;
    private static final double START_POINTS = 100;

    @Before
    public void setUp() throws Exception {
        map = mock(Map.class);
        dice = mock(Dice.class);
        startPoint = mock(Land.class);
    }

    @Test
    public void should_wait_command_when_sell_tool() throws Exception {
        Player player = Player.createPlayerWithRobot(1, map, dice, startPoint);

        player.sellTool(Tool.Type.ROBOT);

        assertThat(player.getStatus(), is(Player.Status.WAIT_COMMAND));
    }

    @Test
    public void should_decrease_tool_and_increase_points_when_sell_tool() throws Exception {
        Player player = Player.createPlayerWithRobot(1, map, dice, startPoint);
        int preToolSum = player.getTools().size();
        int prePoints = player.getPoints();
        player.sellTool(Tool.Type.ROBOT);

        assertThat(player.getTools().size(), is(preToolSum - 1));
        assertThat(player.getPoints(), is(prePoints + Tool.Type.ROBOT.getPointPrice()));
    }
}