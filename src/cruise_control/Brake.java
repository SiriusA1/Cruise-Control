package cruise_control;

public class Brake {
	
	//basically whether the sensor is activated or not.
	//false = unpressed, true = pressed
	private boolean brakeState;
	
	protected Brake() {
		this.brakeState = false;
	}
	protected boolean getState() {
		return brakeState;
	}
	protected void updateState() {
		this.brakeState = !this.brakeState;
	}

}
