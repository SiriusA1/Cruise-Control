package cruise_control;

public class Engine {
	private boolean power;
	protected Engine() {
		this.power = true;
	}
	protected boolean getState() {
		return power;
	}
	protected void updateState() {
		this.power = !this.power;
	}
}
