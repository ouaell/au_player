package app.auplayer.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateMachineTest {
    @Test
    fun `starts idle`() {
        assertEquals(PlayerState.Idle, PlayerStateMachine().state)
    }

    @Test
    fun `happy path reaches playing`() {
        val machine = PlayerStateMachine()
        machine.transition(PlayerState.Loading)
        machine.transition(PlayerState.Ready)
        machine.transition(PlayerState.Playing)
        assertEquals(PlayerState.Playing, machine.state)
    }

    @Test
    fun `play pause buffering cycle holds`() {
        val machine = PlayerStateMachine()
        machine.transition(PlayerState.Loading)
        machine.transition(PlayerState.Ready)
        machine.transition(PlayerState.Playing)
        machine.transition(PlayerState.Paused)
        machine.transition(PlayerState.Buffering)
        machine.transition(PlayerState.Playing)
        assertEquals(PlayerState.Playing, machine.state)
    }

    @Test
    fun `error recovers through loading`() {
        val machine = PlayerStateMachine()
        machine.transition(PlayerState.Loading)
        machine.transition(PlayerState.Error)
        machine.transition(PlayerState.Loading)
        machine.transition(PlayerState.Ready)
        assertEquals(PlayerState.Ready, machine.state)
    }

    @Test
    fun `illegal transitions throw`() {
        val illegal = listOf(
            PlayerState.Idle to PlayerState.Playing,
            PlayerState.Idle to PlayerState.Released,
            PlayerState.Loading to PlayerState.Playing,
            PlayerState.Playing to PlayerState.Idle,
            PlayerState.Playing to PlayerState.Ready,
            PlayerState.Paused to PlayerState.Idle,
            PlayerState.Error to PlayerState.Playing,
            PlayerState.Released to PlayerState.Loading,
        )
        illegal.forEach { (from, to) ->
            val machine = PlayerStateMachine()
            driveTo(machine, from)
            assertThrows(IllegalStateException::class.java) { machine.transition(to) }
            assertEquals(from, machine.state)
        }
    }

    @Test
    fun `released is terminal`() {
        assertTrue(PlayerStateMachine.allowed(PlayerState.Released).isEmpty())
    }

    @Test
    fun `ui state progress guards zero duration`() {
        assertEquals(0f, PlayerUiState(positionMs = 5_000, durationMs = 0).progress)
        assertEquals(0.5f, PlayerUiState(positionMs = 5_000, durationMs = 10_000).progress)
    }

    private fun driveTo(machine: PlayerStateMachine, target: PlayerState) {
        when (target) {
            PlayerState.Idle -> Unit
            PlayerState.Loading -> machine.transition(PlayerState.Loading)
            PlayerState.Ready -> {
                machine.transition(PlayerState.Loading)
                machine.transition(PlayerState.Ready)
            }
            PlayerState.Playing -> {
                driveTo(machine, PlayerState.Ready)
                machine.transition(PlayerState.Playing)
            }
            PlayerState.Paused -> {
                driveTo(machine, PlayerState.Playing)
                machine.transition(PlayerState.Paused)
            }
            PlayerState.Buffering -> {
                driveTo(machine, PlayerState.Playing)
                machine.transition(PlayerState.Buffering)
            }
            PlayerState.Error -> {
                machine.transition(PlayerState.Loading)
                machine.transition(PlayerState.Error)
            }
            PlayerState.Released -> {
                driveTo(machine, PlayerState.Ready)
                machine.transition(PlayerState.Released)
            }
        }
    }
}
